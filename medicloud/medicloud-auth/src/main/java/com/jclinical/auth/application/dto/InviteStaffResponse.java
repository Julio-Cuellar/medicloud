package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta tras enviar exitosamente una invitación de staff.
 * Contiene el estado del envío y las credenciales del doctor si aplica.
 * Retornado por el endpoint {@code POST /v1/staff/invite} con código {@code 201}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteStaffResponse {

    /** Indica si la invitación fue enviada correctamente. */
    @JsonProperty("invitation_sent")
    private boolean invitationSent;

    /** Correo electrónico del invitado. */
    private String email;

    /** Rol asignado al personal en la clínica. */
    private String role;

    /**
     * Credenciales profesionales del doctor con el estado de verificación calculado.
     * Solo presente cuando {@code role} sea {@code doctor}.
     */
    @JsonProperty("doctor_credentials")
    private DoctorCredentialsResponseDto doctorCredentials;

    /**
     * DTO anidado con los datos de credenciales del doctor más el estado de verificación.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorCredentialsResponseDto {

        /** Institución de egreso del médico. */
        @JsonProperty("universidad_egreso")
        private String universidadEgreso;

        /** Año de egreso del médico. */
        @JsonProperty("anio_egreso")
        private Integer anioEgreso;

        /** Especialidad médica. */
        private String especialidad;

        /** Subespecialidad médica. */
        @JsonProperty("sub_especialidad")
        private String subEspecialidad;

        /** Cédula profesional SEP. */
        @JsonProperty("cedula_profesional")
        private String cedulaProfesional;

        /** Cédula de especialidad. */
        @JsonProperty("cedula_especialidad")
        private String cedulaEspecialidad;

        /** Institución que otorgó la especialidad. */
        @JsonProperty("institucion_especialidad")
        private String institucionEspecialidad;

        /** URL del documento de trámite de cédula. */
        @JsonProperty("documento_tramite_url")
        private String documentoTramiteUrl;

        /** Estado de la credencial: {@code activo} o {@code en_tramite}. */
        @JsonProperty("credential_status")
        private String credentialStatus;
    }
}
