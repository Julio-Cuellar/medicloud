package com.jclinical.auth.application.controller;

import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.service.StaffService;
import com.jclinical.shared.domain.ApiResponse;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de personal (staff) de una clínica.
 * <p>
 * Todos los endpoints requieren el header {@code X-Clinic-ID} con el UUID de la
 * clínica activa, conforme al contrato API_Modulo_01_Auth.md v2.1.
 * </p>
 */
@RestController
@RequestMapping("/v1/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService staffService;

    /**
     * Envía una invitación de staff a un nuevo empleado de la clínica activa.
     * <p>
     * Si el rol asignado es {@code doctor}, se requieren las credenciales profesionales
     * conforme a la NOM-004-SSA3-2012. Requiere rol de administrador en la clínica.
     * </p>
     *
     * @param clinicIdHeader Header {@code X-Clinic-ID} con el UUID de la clínica activa.
     * @param jwt            Token JWT del usuario autenticado (administrador).
     * @param request        Datos del invitado: email, nombre, rol y credenciales opcionales.
     * @return Respuesta {@code 201} con el estado del envío y las credenciales calculadas.
     */
    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InviteStaffResponse> invite(
            @RequestHeader("X-Clinic-ID") String clinicIdHeader,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody InviteStaffRequest request) {
        UUID clinicId = parseClinicId(clinicIdHeader);
        UUID invitedById = UUID.fromString(jwt.getSubject());
        return ApiResponse.success(staffService.inviteStaff(clinicId, invitedById, request));
    }

    /**
     * Obtiene el perfil detallado de un miembro del staff de la clínica activa.
     * <p>
     * Si el miembro tiene rol {@code doctor}, la respuesta incluye el campo
     * {@code doctor_credentials} con toda la información de sus credenciales profesionales.
     * </p>
     *
     * @param clinicIdHeader Header {@code X-Clinic-ID} con el UUID de la clínica activa.
     * @param id             UUID del miembro del staff a consultar.
     * @return Respuesta {@code 200} con el perfil completo del miembro.
     */
    @GetMapping("/{id}")
    public ApiResponse<StaffResponse> getStaff(
            @RequestHeader("X-Clinic-ID") String clinicIdHeader,
            @PathVariable UUID id) {
        UUID clinicId = parseClinicId(clinicIdHeader);
        return ApiResponse.success(staffService.getStaff(clinicId, id));
    }

    /**
     * Verifica y marca como válidas las credenciales profesionales de un médico.
     * <p>
     * Solo puede ser ejecutado por un administrador de clínica ({@code clinic_admin}).
     * Cambia el estado de la credencial a {@code activo} si hay cédula profesional,
     * o mantiene {@code en_tramite} si solo existe documento comprobatorio.
     * </p>
     *
     * @param clinicIdHeader Header {@code X-Clinic-ID} con el UUID de la clínica activa.
     * @param id             UUID del miembro del staff (debe tener rol {@code doctor}).
     * @param jwt            Token JWT del usuario administrador que realiza la verificación.
     * @return Respuesta {@code 200} con el estado actualizado de la credencial.
     */
    @PatchMapping("/{id}/credentials/verify")
    public ApiResponse<VerifyCredentialsResponse> verifyCredentials(
            @RequestHeader("X-Clinic-ID") String clinicIdHeader,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID clinicId = parseClinicId(clinicIdHeader);
        UUID verifiedByUserId = UUID.fromString(jwt.getSubject());
        return ApiResponse.success(staffService.verifyCredentials(clinicId, id, verifiedByUserId));
    }

    /**
     * Parsea y valida el valor del header {@code X-Clinic-ID} como un UUID.
     *
     * @param clinicIdHeader Valor del header como cadena de texto.
     * @return UUID de la clínica activa.
     * @throws MedicloudException Si el valor no es un UUID válido.
     */
    private UUID parseClinicId(String clinicIdHeader) {
        try {
            return UUID.fromString(clinicIdHeader);
        } catch (IllegalArgumentException e) {
            throw new MedicloudException(
                    "El header X-Clinic-ID debe ser un UUID válido.",
                    ErrorCodes.VALIDATION_ERROR, 422);
        }
    }
}
