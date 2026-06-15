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
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenHelper tokenHelper;

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
