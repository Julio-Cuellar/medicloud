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
