package com.jclinical.auth.application.controller;

import com.jclinical.auth.application.dto.TokenResponse;
import com.jclinical.auth.application.dto.UserResponse;
import com.jclinical.auth.application.service.AuthenticationService;
import com.jclinical.auth.infrastructure.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuth2Controller.class)
@Import(SecurityConfig.class)
public class OAuth2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void testTokenPasswordGrantSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("test@medicloud.mx")
                .fullName("Test User")
                .clinics(Collections.emptyList())
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("mock-access-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken("mock-refresh-token")
                .user(userResponse)
                .build();

        when(authenticationService.login(
                eq("test@medicloud.mx"),
                eq("Password123!"),
                eq("Web App"),
                anyString(),
                anyString()
        )).thenReturn(tokenResponse);

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", "test@medicloud.mx")
                        .param("password", "Password123!")
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("mock-access-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@medicloud.mx"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=mock-refresh-token")));
    }

    @Test
    public void testTokenRefreshTokenGrantSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("test@medicloud.mx")
                .fullName("Test User")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-mock-access-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken("new-mock-refresh-token")
                .user(userResponse)
                .build();

        when(authenticationService.refresh(
                eq("old-mock-refresh-token"),
                eq("Web App"),
                anyString(),
                anyString()
        )).thenReturn(tokenResponse);

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "refresh_token")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-mock-refresh-token"))
                        .header("User-Agent", "TestAgent")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("new-mock-access-token"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=new-mock-refresh-token")));
    }

    @Test
    public void testTokenInvalidGrantType() throws Exception {
        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
