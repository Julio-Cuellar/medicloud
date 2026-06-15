package com.jclinical.auth.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.service.StaffService;
import com.jclinical.auth.infrastructure.SecurityConfig;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
@Import(SecurityConfig.class)
public class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StaffService staffService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void testInviteStaffSuccess() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        InviteStaffRequest request = new InviteStaffRequest();
        request.setEmail("doctor@medicloud.mx");
        request.setFullName("Dr. Gregory House");
        request.setRole("doctor");

        InviteStaffResponse response = InviteStaffResponse.builder()
                .invitationSent(true)
                .email("doctor@medicloud.mx")
                .role("doctor")
                .build();

        when(staffService.inviteStaff(eq(clinicId), eq(adminId), any(InviteStaffRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/staff/invite")
                        .with(jwt().jwt(builder -> builder.subject(adminId.toString())))
                        .header("X-Clinic-ID", clinicId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invitation_sent").value(true))
                .andExpect(jsonPath("$.data.email").value("doctor@medicloud.mx"));
    }

    @Test
    public void testInviteStaffForbidden() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        InviteStaffRequest request = new InviteStaffRequest();
        request.setEmail("doctor2@medicloud.mx");
        request.setFullName("Dr. John Watson");
        request.setRole("doctor");

        when(staffService.inviteStaff(eq(clinicId), eq(doctorId), any(InviteStaffRequest.class)))
                .thenThrow(new MedicloudException("Solo un administrador puede invitar miembros al staff.", ErrorCodes.FORBIDDEN, 403));

        mockMvc.perform(post("/v1/staff/invite")
                        .with(jwt().jwt(builder -> builder.subject(doctorId.toString())))
                        .header("X-Clinic-ID", clinicId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCodes.FORBIDDEN))
                .andExpect(jsonPath("$.error.message").value("Solo un administrador puede invitar miembros al staff."));
    }

    @Test
    public void testInviteStaffNotInClinic() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        InviteStaffRequest request = new InviteStaffRequest();
        request.setEmail("doctor3@medicloud.mx");
        request.setFullName("Dr. Stephen Strange");
        request.setRole("doctor");

        when(staffService.inviteStaff(eq(clinicId), eq(strangerId), any(InviteStaffRequest.class)))
                .thenThrow(new MedicloudException("El usuario que invita no pertenece a la clínica activa.", ErrorCodes.CLINIC_ACCESS_DENIED, 403));

        mockMvc.perform(post("/v1/staff/invite")
                        .with(jwt().jwt(builder -> builder.subject(strangerId.toString())))
                        .header("X-Clinic-ID", clinicId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCodes.CLINIC_ACCESS_DENIED));
    }

    @Test
    public void testVerifyCredentialsSuccess() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        VerifyCredentialsResponse response = VerifyCredentialsResponse.builder()
                .staffId(staffId)
                .credentialStatus("activo")
                .verifiedAt(Instant.now())
                .build();

        when(staffService.verifyCredentials(eq(clinicId), eq(staffId), eq(adminId)))
                .thenReturn(response);

        mockMvc.perform(patch("/v1/staff/{id}/credentials/verify", staffId)
                        .with(jwt().jwt(builder -> builder.subject(adminId.toString())))
                        .header("X-Clinic-ID", clinicId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_id").value(staffId.toString()))
                .andExpect(jsonPath("$.data.credential_status").value("activo"));
    }

    @Test
    public void testVerifyCredentialsForbidden() throws Exception {
        UUID clinicId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        when(staffService.verifyCredentials(eq(clinicId), eq(staffId), eq(doctorId)))
                .thenThrow(new MedicloudException("Solo un administrador de clínica puede verificar credenciales.", ErrorCodes.FORBIDDEN, 403));

        mockMvc.perform(patch("/v1/staff/{id}/credentials/verify", staffId)
                        .with(jwt().jwt(builder -> builder.subject(doctorId.toString())))
                        .header("X-Clinic-ID", clinicId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCodes.FORBIDDEN));
    }
}
