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

/**
 * Controlador REST para gestionar la autenticación tradicional del usuario.
 * <p>
 * Proporciona endpoints para registro, verificación de correo, cierre de sesión,
 * y recuperación/cambio de contraseñas.
 * </p>
 */
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

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request Datos del registro del usuario.
     * @return Respuesta que contiene un mensaje de confirmación de registro.
     */
    @PostMapping("/register")
    public ApiResponse<MessageResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    /**
     * Verifica la dirección de correo electrónico del usuario mediante un token.
     *
     * @param request Datos para la verificación del correo (email y token).
     * @return Respuesta que contiene detalles del resultado de la verificación.
     */
    @PostMapping("/verify-email")
    public ApiResponse<VerifyEmailResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(registrationService.verifyEmail(request));
    }

    /**
     * Reenvía el correo de verificación al usuario.
     *
     * @param request Datos de la solicitud de reenvío de verificación.
     * @return Respuesta que contiene un mensaje confirmando el envío.
     */
    @PostMapping("/resend-verification")
    public ApiResponse<MessageResponse> resendVerification(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(registrationService.resendVerification(request));
    }

    /**
     * Cierra la sesión activa invalidando el token de refresco provisto en la cookie.
     *
     * @param cookieRefreshToken Valor del token de refresco extraído de la cookie.
     * @param response           Objeto de respuesta HTTP para limpiar la cookie.
     * @return Respuesta vacía con estado 204 No Content.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response) {
        authenticationService.logout(cookieRefreshToken);
        setRefreshTokenCookie(response, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cierra todas las sesiones activas del usuario actual (invalida todos sus refresh tokens).
     *
     * @param jwt      Token JWT autenticado del usuario.
     * @param response Objeto de respuesta HTTP para limpiar la cookie de refresco.
     * @return Respuesta vacía con estado 204 No Content.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authenticationService.logoutAll(userId);
        setRefreshTokenCookie(response, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Solicita el restablecimiento de contraseña enviando un correo con un token seguro.
     *
     * @param request Datos de la solicitud de restablecimiento (contiene el correo).
     * @return Respuesta que contiene un mensaje de confirmación de envío.
     */
    @PostMapping("/request-password-reset")
    public ApiResponse<MessageResponse> requestPasswordReset(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(passwordService.requestPasswordReset(request));
    }

    /**
     * Restablece la contraseña del usuario utilizando un token de restablecimiento válido.
     *
     * @param request Datos para restablecer la contraseña (token y nueva contraseña).
     * @return Respuesta que contiene un mensaje del resultado de la operación.
     */
    @PostMapping("/reset-password")
    public ApiResponse<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(passwordService.resetPassword(request));
    }

    /**
     * Cambia la contraseña del usuario autenticado actual.
     *
     * @param jwt                Token JWT autenticado del usuario.
     * @param request            Datos del cambio de contraseña (contraseña actual y nueva).
     * @param cookieRefreshToken Token de refresco de la sesión actual, opcional para invalidación o validación.
     * @return Respuesta que contiene un mensaje confirmando el cambio.
     */
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

    /**
     * Obtiene el perfil detallado del usuario autenticado actual.
     *
     * @param jwt Token JWT autenticado del usuario.
     * @return Respuesta con el perfil del usuario.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(profileService.getProfile(user));
    }

    /**
     * Actualiza el perfil del usuario autenticado actual.
     *
     * @param jwt     Token JWT autenticado del usuario.
     * @param request Datos de actualización del usuario.
     * @return Respuesta con el perfil actualizado.
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(profileService.updateProfile(user, request));
    }

    /**
     * Configura la cookie HttpOnly para el refresh token.
     *
     * @param response           Objeto HttpServletResponse para agregar la cabecera del cookie.
     * @param refreshTokenValue Valor del refresh token. Si es nulo, se limpia la cookie.
     */
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
