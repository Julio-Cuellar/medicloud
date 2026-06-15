package com.jclinical.auth.application.controller;

import com.jclinical.auth.application.dto.TokenResponse;
import com.jclinical.auth.application.service.AuthenticationService;
import com.jclinical.shared.domain.ApiResponse;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controlador REST para el protocolo OAuth2.
 * <p>
 * Gestiona el intercambio de credenciales para la obtención y refresco de tokens
 * de acceso/ID de usuario utilizando los flujos (grants) soportados.
 * </p>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final AuthenticationService authenticationService;

    /**
     * Endpoint unificado de autenticación (/token) compatible con OAuth2.
     * Soporta los grants: {@code password} (inicio de sesión) y {@code refresh_token} (renovación de sesión).
     *
     * @param params             Parámetros de la solicitud del flujo OAuth2.
     * @param cookieRefreshToken Token de refresco recibido a través de una cookie segura.
     * @param request            Detalles de la solicitud HTTP (usado para rastrear IP y User-Agent).
     * @param response           Objeto de respuesta HTTP (usado para establecer la cookie segura).
     * @return Respuesta estructurada con los tokens generados.
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<TokenResponse> token(
            @RequestParam Map<String, String> params,
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        String grantType = params.get("grant_type");
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }

        TokenResponse tokenResponse;
        if ("password".equalsIgnoreCase(grantType)) {
            String username = params.get("username");
            String password = params.get("password");
            tokenResponse = authenticationService.login(username, password, "Web App", ipAddress, userAgent);
        } else if ("refresh_token".equalsIgnoreCase(grantType)) {
            tokenResponse = authenticationService.refresh(cookieRefreshToken, "Web App", ipAddress, userAgent);
        } else {
            throw new MedicloudException("Tipo de grant no soportado", ErrorCodes.VALIDATION_ERROR, 422);
        }

        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success(tokenResponse);
    }

    /**
     * Configura la cookie segura e HttpOnly con el valor del Refresh Token.
     *
     * @param response           Objeto HttpServletResponse para agregar la cabecera de la cookie.
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
