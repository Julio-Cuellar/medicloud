package com.jclinical.auth.application.service;

import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.helper.PasswordValidator;
import com.jclinical.auth.application.helper.TokenHelper;
import com.jclinical.auth.domain.*;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Servicio encargado de gestionar el ciclo de vida de las contraseñas.
 * <p>
 * Incluye la solicitud de restablecimiento, la aplicación del cambio de contraseña
 * a través de tokens seguros y el cambio de contraseña de usuarios autenticados.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenHelper tokenHelper;

    /**
     * Solicita un restablecimiento de contraseña para un correo electrónico dado.
     * <p>
     * Si el usuario existe y está activo, genera un token seguro de restablecimiento,
     * lo guarda en la base de datos y simula (o realiza en el log) el envío de correo.
     * Por motivos de seguridad, siempre retorna un mensaje exitoso genérico.
     * </p>
     *
     * @param request DTO con la dirección de correo electrónico del usuario.
     * @return {@link MessageResponse} indicando que se enviaron instrucciones de ser registrado.
     */
    @Transactional
    public MessageResponse requestPasswordReset(ResendVerificationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isActive()) {
                String rawToken = tokenHelper.generateRandomToken();
                String tokenHash = tokenHelper.hashToken(rawToken);

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .build();

                passwordResetTokenRepository.save(resetToken);
                log.info("Password reset requested for {}. Token: {}", email, rawToken);
            }
        }

        return new MessageResponse("Si el correo está registrado, recibirás las instrucciones para restablecer tu contraseña.");
    }

    /**
     * Restablece la contraseña de un usuario mediante un token previamente generado.
     * <p>
     * Valida que el token exista, no haya expirado y no se haya utilizado previamente.
     * Valida que la contraseña cumpla con las políticas de complejidad e historial del sistema.
     * Tras cambiar la contraseña, invalida todas las sesiones de login activas de ese usuario.
     * </p>
     *
     * @param request DTO con el token y la nueva contraseña.
     * @return {@link MessageResponse} confirmando que la contraseña fue actualizada.
     * @throws MedicloudException Si el token es inválido, ya usado, ha expirado o si la contraseña no cumple las políticas.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = tokenHelper.hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MedicloudException("Token de restablecimiento inválido o ya utilizado", ErrorCodes.RESET_TOKEN_INVALID, 401));

        if (resetToken.getUsedAt() != null) {
            throw new MedicloudException("Token de restablecimiento inválido o ya utilizado", ErrorCodes.RESET_TOKEN_INVALID, 401);
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new MedicloudException("Token de restablecimiento vencido (más de 1 h)", ErrorCodes.RESET_TOKEN_EXPIRED, 401);
        }

        User user = resetToken.getUser();
        passwordValidator.validatePasswordStrength(request.getNewPassword());
        passwordValidator.validatePasswordHistory(user, request.getNewPassword());

        PasswordHistory history = PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPasswordHash())
                .build();
        passwordHistoryRepository.save(history);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getId());

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        log.info("Reset password completed for user: {}", user.getEmail());
        return new MessageResponse("Contraseña actualizada correctamente. Inicia sesión con tu nueva contraseña.");
    }

    /**
     * Permite cambiar la contraseña de un usuario autenticado activo.
     * <p>
     * Valida la contraseña actual del usuario, realiza las validaciones de complejidad
     * e historial de la nueva contraseña, y finalmente guarda los cambios cerrando
     * todas las sesiones activas en otros dispositivos.
     * </p>
     *
     * @param user                   El usuario autenticado que solicita cambiar de contraseña.
     * @param request                DTO con la contraseña actual y la nueva contraseña.
     * @param currentRawRefreshToken El token de refresco de la sesión actual (opcional).
     * @return {@link MessageResponse} de confirmación.
     * @throws MedicloudException Si la contraseña actual es incorrecta o si la nueva no cumple con las políticas.
     */
    @Transactional
    public MessageResponse changePassword(User user, ChangePasswordRequest request, String currentRawRefreshToken) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new MedicloudException("La contraseña actual es incorrecta", ErrorCodes.INVALID_CREDENTIALS, 401);
        }

        passwordValidator.validatePasswordStrength(request.getNewPassword());
        passwordValidator.validatePasswordHistory(user, request.getNewPassword());

        PasswordHistory history = PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPasswordHash())
                .build();
        passwordHistoryRepository.save(history);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getId());

        log.info("Changed password for user: {}", user.getEmail());
        return new MessageResponse("Contraseña actualizada. Las otras sesiones activas han sido cerradas.");
    }
}
