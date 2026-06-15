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
 * Servicio encargado de gestionar el registro de nuevos usuarios en la plataforma.
 * <p>
 * Incluye la validación inicial de contraseñas, la creación de la cuenta de usuario,
 * la generación y persistencia de tokens de verificación de correo electrónico,
 * y el procesamiento de la verificación del correo electrónico.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenHelper tokenHelper;

    /**
     * Registra un nuevo usuario en la base de datos con estado de correo no verificado.
     * <p>
     * Valida que la contraseña cumpla con las políticas del sistema. Si el correo
     * electrónico ya está registrado, retorna un mensaje genérico exitoso de forma silenciosa
     * para evitar enumeración de cuentas.
     * </p>
     *
     * @param request DTO con la información de registro del nuevo usuario.
     * @return {@link MessageResponse} indicando que recibirá instrucciones de verificación.
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        passwordValidator.validatePasswordStrength(request.getPassword());

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            log.info("Email already exists: {}. Silently succeeding.", email);
            return new MessageResponse("Si el correo ingresado no está registrado previamente, recibirás un enlace de verificación en breve.");
        }

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .themePreference("light")
                .isActive(true)
                .build();

        userRepository.save(user);

        String rawToken = tokenHelper.generateRandomToken();
        String tokenHash = tokenHelper.hashToken(rawToken);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        log.info("Registered user: {}. Verification token: {}", email, rawToken);
        return new MessageResponse("Si el correo ingresado no está registrado previamente, recibirás un enlace de verificación en breve.");
    }

    /**
     * Verifica la dirección de correo electrónico del usuario marcando su cuenta
     * como verificada en la base de datos si el token es válido.
     *
     * @param request DTO que contiene el token de verificación.
     * @return {@link VerifyEmailResponse} confirmando que el correo fue verificado.
     * @throws MedicloudException Si el token es inválido, ya fue usado o ha expirado.
     */
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String tokenHash = tokenHelper.hashToken(request.getToken());
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MedicloudException("Token de verificación inválido o ya utilizado", ErrorCodes.VERIFICATION_TOKEN_INVALID, 401));

        if (verificationToken.getUsedAt() != null) {
            throw new MedicloudException("Token de verificación inválido o ya utilizado", ErrorCodes.VERIFICATION_TOKEN_INVALID, 401);
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new MedicloudException("Token de verificación vencido", ErrorCodes.VERIFICATION_TOKEN_EXPIRED, 401);
        }

        verificationToken.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Verified email for user: {}", user.getEmail());
        return new VerifyEmailResponse("Correo verificado correctamente. Ya puedes iniciar sesión.", user.getEmail());
    }

    /**
     * Reenvía un nuevo correo y token de verificación si la cuenta asociada al email
     * no ha sido verificada aún.
     *
     * @param request DTO con el correo electrónico del usuario.
     * @return {@link MessageResponse} indicando que recibirá instrucciones de verificación.
     */
    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isEmailVerified()) {
                String rawToken = tokenHelper.generateRandomToken();
                String tokenHash = tokenHelper.hashToken(rawToken);

                EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                        .build();

                emailVerificationTokenRepository.save(verificationToken);
                log.info("Resent verification for: {}. Token: {}", email, rawToken);
            }
        }

        return new MessageResponse("Si el correo está registrado y pendiente de verificación, recibirás un nuevo enlace en breve.");
    }
}
