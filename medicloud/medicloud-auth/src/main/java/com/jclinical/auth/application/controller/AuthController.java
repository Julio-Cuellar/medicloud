package com.jclinical.auth.application.controller;

import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.service.*;
import com.jclinical.auth.domain.User;
import com.jclinical.auth.domain.UserRepository;
import com.jclinical.shared.domain.ApiResponse;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ApiResponse<MessageResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    @PostMapping("/verify-email")
    public ApiResponse<VerifyEmailResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(registrationService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ApiResponse<MessageResponse> resendVerification(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(registrationService.resendVerification(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response) {
        authenticationService.logout(cookieRefreshToken);
        setRefreshTokenCookie(response, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authenticationService.logoutAll(userId);
        setRefreshTokenCookie(response, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-password-reset")
    public ApiResponse<MessageResponse> requestPasswordReset(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(passwordService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(passwordService.resetPassword(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<MessageResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequest request,
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(passwordService.changePassword(user, request, cookieRefreshToken));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(profileService.getProfile(user));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(profileService.updateProfile(user, request));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshTokenValue) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshTokenValue != null ? refreshTokenValue : "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/oauth2")
                .maxAge(refreshTokenValue == null || refreshTokenValue.isBlank() ? 0 : 2592000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
