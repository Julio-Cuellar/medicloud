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
 * Expone el endpoint {@code POST /oauth2/token} para autenticación con credenciales
 * (grant_type=password) y renovación de sesión (grant_type=refresh_token),
 * conforme al contrato API_Modulo_01_Auth.md v2.1.
 * </p>
 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final AuthenticationService authenticationService;

    /**
     * Endpoint unificado de autenticación compatible con OAuth2.
     * Soporta los grants: {@code password} (inicio de sesión) y {@code refresh_token} (renovación de sesión).
     *
     * @param params             Parámetros de la solicitud del flujo OAuth2 (form-encoded).
     * @param cookieRefreshToken Token de refresco recibido a través de cookie segura {@code HttpOnly}.
     * @param request            Detalles de la solicitud HTTP (IP, User-Agent).
     * @param response           Objeto de respuesta HTTP para establecer la cookie de refresco.
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
     * Configura la cookie {@code HttpOnly} para el refresh token.
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
