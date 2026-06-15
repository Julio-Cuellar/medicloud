package com.jclinical.auth.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.service.*;
import com.jclinical.auth.domain.User;
import com.jclinical.auth.domain.UserRepository;
import com.jclinical.auth.infrastructure.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Julio Cuellar");
        request.setEmail("julio@medicloud.mx");
        request.setPhone("1234567890");
        request.setPassword("Password123!");

        MessageResponse response = new MessageResponse("Usuario registrado exitosamente");

        when(registrationService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Usuario registrado exitosamente"));
    }

    @Test
    public void testVerifyEmailSuccess() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("verification-token");

        VerifyEmailResponse response = new VerifyEmailResponse();
        response.setMessage("Email verificado");
        response.setEmail("julio@medicloud.mx");

        when(registrationService.verifyEmail(any(VerifyEmailRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Email verificado"));
    }

    @Test
    public void testResendVerificationSuccess() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("julio@medicloud.mx");

        MessageResponse response = new MessageResponse("Verificación enviada");

        when(registrationService.resendVerification(any(ResendVerificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Verificación enviada"));
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .with(jwt())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "some-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));

        verify(authenticationService).logout("some-refresh-token");
    }

    @Test
    public void testLogoutAllSuccess() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/v1/auth/logout-all")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        verify(authenticationService).logoutAll(userId);
    }

    @Test
    public void testRequestPasswordResetSuccess() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("julio@medicloud.mx");

        MessageResponse response = new MessageResponse("Correo de recuperación enviado");

        when(passwordService.requestPasswordReset(any(ResendVerificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Correo de recuperación enviado"));
    }

    @Test
    public void testResetPasswordSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("NewPassword123!");

        MessageResponse response = new MessageResponse("Contraseña restablecida exitosamente");

        when(passwordService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Contraseña restablecida exitosamente"));
    }

    @Test
    public void testChangePasswordSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .email("julio@medicloud.mx")
                .fullName("Julio Cuellar")
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword123!");
        request.setNewPassword("NewPassword123!");

        MessageResponse response = new MessageResponse("Contraseña modificada exitosamente");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(passwordService.changePassword(eq(mockUser), any(ChangePasswordRequest.class), eq("mock-refresh"))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/change-password")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "mock-refresh"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Contraseña modificada exitosamente"));
    }

    @Test
    public void testMeSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .email("julio@medicloud.mx")
                .fullName("Julio Cuellar")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("julio@medicloud.mx")
                .fullName("Julio Cuellar")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(profileService.getProfile(mockUser)).thenReturn(userResponse);

        mockMvc.perform(get("/v1/auth/me")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("julio@medicloud.mx"))
                .andExpect(jsonPath("$.data.full_name").value("Julio Cuellar"));
    }

    @Test
    public void testUpdateMeSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .email("julio@medicloud.mx")
                .fullName("Julio Cuellar")
                .build();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Julio Cuellar Editado");
        request.setPhone("0987654321");
        request.setThemePreference("dark");

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("julio@medicloud.mx")
                .fullName("Julio Cuellar Editado")
                .phone("0987654321")
                .themePreference("dark")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(profileService.updateProfile(eq(mockUser), any(UpdateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(patch("/v1/auth/me")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Julio Cuellar Editado"))
                .andExpect(jsonPath("$.data.theme_preference").value("dark"));
    }
}
