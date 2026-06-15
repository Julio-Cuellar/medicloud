package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta con el perfil detallado de un miembro del staff.
 * Retornado por el endpoint {@code GET /v1/staff/{id}}.
 * Si el miembro tiene rol {@code doctor}, incluye el campo {@code doctor_credentials}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {

    /** Identificador único del miembro del staff. */
    private UUID id;

    /** Nombre completo del miembro. */
    @JsonProperty("full_name")
    private String fullName;

    /** Correo electrónico del miembro. */
    private String email;

    /** Rol en la clínica (ej. {@code doctor}, {@code receptionist}). */
    private String role;

    /** Etiqueta legible del rol en español. */
    @JsonProperty("role_label")
    private String roleLabel;

    /** Indica si el miembro está activo en la clínica. */
    @JsonProperty("is_active")
    private boolean isActive;

    /**
     * Credenciales profesionales del doctor.
     * Solo presente cuando {@code role} sea {@code doctor}; {@code null} en otros casos.
     */
    @JsonProperty("doctor_credentials")
    private DoctorCredentialsDetailDto doctorCredentials;

    /**
     * DTO anidado con los datos completos de credenciales del médico, incluyendo
     * información de verificación administrativa.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorCredentialsDetailDto {

        /** Cédula profesional SEP. */
        @JsonProperty("cedula_profesional")
        private String cedulaProfesional;

        /** Cédula de especialidad médica. */
        @JsonProperty("cedula_especialidad")
        private String cedulaEspecialidad;

        /** Especialidad médica. */
        private String especialidad;

        /** Subespecialidad médica. */
        @JsonProperty("sub_especialidad")
        private String subEspecialidad;

        /** Institución de egreso del médico. */
        @JsonProperty("universidad_egreso")
        private String universidadEgreso;

        /** Año de egreso del médico. */
        @JsonProperty("anio_egreso")
        private Integer anioEgreso;

        /** Institución que otorgó la especialidad. */
        @JsonProperty("institucion_especialidad")
        private String institucionEspecialidad;

        /** Estado de la credencial: {@code activo}, {@code en_tramite} o {@code suspendido}. */
        @JsonProperty("credential_status")
        private String credentialStatus;

        /** Fecha y hora en que fue verificada la credencial por un administrador. */
        @JsonProperty("verified_at")
        private Instant verifiedAt;

        /** ID del usuario administrador que verificó la credencial. */
        @JsonProperty("verified_by_user_id")
        private UUID verifiedByUserId;
    }
}
