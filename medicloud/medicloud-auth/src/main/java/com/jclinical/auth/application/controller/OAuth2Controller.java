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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final AuthenticationService authenticationService;

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
