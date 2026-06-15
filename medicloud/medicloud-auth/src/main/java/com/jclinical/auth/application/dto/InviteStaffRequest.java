package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para el cuerpo de la solicitud de invitación de un nuevo miembro del staff.
 * Utilizado por el endpoint {@code POST /v1/staff/invite}.
 */
@Data
public class InviteStaffRequest {

    /** Correo electrónico del invitado. */
    private String email;

    /** Nombre completo del invitado. */
    @JsonProperty("full_name")
    private String fullName;

    /**
     * Rol asignado al personal en la clínica.
     * Valores válidos: {@code doctor}, {@code receptionist}, {@code assistant},
     * {@code accountant}, {@code cleaning}, {@code admin}, {@code clinic_admin}.
     */
    private String role;

    /**
     * Credenciales profesionales del médico.
     * Requerido obligatoriamente cuando {@code role} sea {@code doctor}.
     */
    @JsonProperty("doctor_credentials")
    private DoctorCredentialsDto doctorCredentials;
}
