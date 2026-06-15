package com.jclinical.auth.application.service;

import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.helper.TokenHelper;
import com.jclinical.auth.domain.*;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final JwtEncoder jwtEncoder;
    private final TokenHelper tokenHelper;

    @Transactional
    public TokenResponse login(String username, String password, String deviceLabel, String ipAddress, String userAgent) {
        String email = username.toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MedicloudException("Credenciales inválidas o cuenta no habilitada", ErrorCodes.INVALID_CREDENTIALS, 401));

        if (!user.isActive()) {
            throw new MedicloudException("Credenciales inválidas o cuenta no habilitada", ErrorCodes.USER_INACTIVE, 403);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new MedicloudException("Cuenta bloqueada temporalmente tras 5 intentos fallidos (15 min)", ErrorCodes.ACCOUNT_LOCKED, 403);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
                log.warn("User account locked due to excessive failed attempts: {}", email);
            }
            userRepository.save(user);
            throw new MedicloudException("Credenciales inválidas o cuenta no habilitada", ErrorCodes.INVALID_CREDENTIALS, 401);
        }

        if (!user.isEmailVerified()) {
            throw new MedicloudException("Credenciales inválidas o cuenta no habilitada", ErrorCodes.EMAIL_NOT_VERIFIED, 403);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return generateTokens(user, deviceLabel, ipAddress, userAgent);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken, String deviceLabel, String ipAddress, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new MedicloudException("Token no existe o ya fue rotado", ErrorCodes.REFRESH_TOKEN_INVALID, 401);
        }

        String tokenHash = tokenHelper.hashToken(rawRefreshToken);
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MedicloudException("Token no existe o ya fue rotado", ErrorCodes.REFRESH_TOKEN_INVALID, 401));

        if (oldToken.getRevokedAt() != null) {
            if (oldToken.getReplacedById() != null) {
                log.warn("Detección de reutilización de refresh token para usuario: {}. Revocando familia.", oldToken.getUser().getEmail());
                refreshTokenRepository.deleteByUserId(oldToken.getUser().getId());
                throw new MedicloudException("Reuso de un token ya rotado; se revocó toda la familia.", ErrorCodes.REFRESH_TOKEN_REUSED, 401);
            }
            throw new MedicloudException("Token no existe o ya fue rotado", ErrorCodes.REFRESH_TOKEN_INVALID, 401);
        }

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new MedicloudException("El token ha expirado", ErrorCodes.REFRESH_TOKEN_EXPIRED, 401);
        }

        User user = oldToken.getUser();
        if (!user.isActive()) {
            throw new MedicloudException("Cuenta inactiva", ErrorCodes.USER_INACTIVE, 403);
        }

        oldToken.setRevokedAt(Instant.now());

        String newRawToken = tokenHelper.generateRandomToken();
        String newTokenHash = tokenHelper.hashToken(newRawToken);
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .deviceLabel(deviceLabel)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(newToken);

        oldToken.setReplacedById(newToken.getId());
        refreshTokenRepository.save(oldToken);

        TokenResponse tokenResponse = generateTokens(user, deviceLabel, ipAddress, userAgent);
        tokenResponse.setRefreshToken(newRawToken);

        return tokenResponse;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenHelper.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private TokenResponse generateTokens(User user, String deviceLabel, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        List<ClinicStaff> staffList = clinicStaffRepository.findByUserId(user.getId());
        List<String> roles = staffList.stream()
                .map(cs -> cs.getRole().getValue())
                .distinct()
                .toList();
        List<String> clinics = staffList.stream()
                .map(cs -> cs.getClinicId().toString())
                .distinct()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://api.medicloud.mx")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("clinics", clinics)
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        String rawRefreshToken = tokenHelper.generateRandomToken();
        String tokenHash = tokenHelper.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceLabel(deviceLabel)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = profileService.getProfile(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken(rawRefreshToken)
                .user(userResponse)
                .build();
    }
}
