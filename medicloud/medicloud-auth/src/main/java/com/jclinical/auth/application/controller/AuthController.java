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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Controlador REST para gestionar la autenticación tradicional del usuario.
 * <p>
 * Proporciona endpoints para registro, verificación de correo, cierre de sesión,
 * recuperación/cambio de contraseñas, perfil de usuario, subida de avatar y
 * aceptación de invitaciones de staff.
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
    private final AvatarService avatarService;
    private final StaffService staffService;
    private final UserRepository userRepository;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request Datos del registro del usuario.
     * @return ApiResponse con confirmación.
     */
    @PostMapping("/register")
    public ApiResponse<MessageResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    /**
     * Verifica la dirección de correo electrónico del usuario mediante un token.
     *
     * @param request Datos para la verificación del correo (token de verificación).
     * @return ApiResponse con detalles de verificación.
     */
    @PostMapping("/verify-email")
    public ApiResponse<VerifyEmailResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(registrationService.verifyEmail(request));
    }

    /**
     * Reenvía el correo de verificación al usuario.
     *
     * @param request Datos de la solicitud de reenvío de verificación (email).
     * @return ApiResponse con confirmación.
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
     * @param request Datos de la solicitud de restablecimiento (correo electrónico).
     * @return ApiResponse con confirmación.
     */
    @PostMapping("/request-password-reset")
    public ApiResponse<MessageResponse> requestPasswordReset(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(passwordService.requestPasswordReset(request));
    }

    /**
     * Restablece la contraseña del usuario utilizando un token de restablecimiento válido.
     *
     * @param request Datos para restablecer la contraseña (token y nueva contraseña).
     * @return ApiResponse con resultado.
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
     * @param cookieRefreshToken Token de refresco de la sesión actual (para preservarla).
     * @return ApiResponse con confirmación.
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
     * @return ApiResponse con perfil.
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
     * @param request Datos de actualización del usuario (nombre, teléfono, tema).
     * @return ApiResponse con perfil actualizado.
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
     * Sube o reemplaza el avatar del usuario autenticado.
     *
     * @param jwt  Token JWT autenticado del usuario.
     * @param file Imagen del avatar (JPG, PNG o WebP, máximo 2 MB).
     * @return ApiResponse con el avatar subido.
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));
        return ApiResponse.success(avatarService.uploadAvatar(user, file));
    }

    /**
     * Acepta una invitación de staff para unirse a una clínica.
     *
     * @param request  Datos de la invitación (token e, opcionalmente, contraseña).
     * @param response Objeto de respuesta HTTP para establecer la cookie de refresco.
     * @return ApiResponse con la aceptación.
     */
    @PostMapping("/accept-invitation")
    public ApiResponse<AcceptInvitationResponse> acceptInvitation(
            @RequestBody AcceptInvitationRequest request,
            HttpServletResponse response) {
        AcceptInvitationResponse result = staffService.acceptInvitation(request);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    /**
     * Configura la cookie HttpOnly para el refresh token.
     *
     * @param response           Objeto HttpServletResponse para agregar la cabecera Set-Cookie.
     * @param refreshTokenValue Valor del refresh token. Si es nulo o vacío, limpia la cookie.
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
