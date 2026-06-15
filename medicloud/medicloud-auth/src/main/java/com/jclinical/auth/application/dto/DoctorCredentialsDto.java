package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO que encapsula las credenciales profesionales de un médico
 * al momento de crear una invitación de staff.
 * Requerido cuando el rol asignado es {@code doctor}.
 */
@Data
public class DoctorCredentialsDto {

    /** Institución de educación superior donde obtuvo el título profesional. */
    @JsonProperty("universidad_egreso")
    private String universidadEgreso;

    /** Año de egreso o titulación del médico. */
    @JsonProperty("anio_egreso")
    private Integer anioEgreso;

    /** Especialidad médica del doctor; {@code null} si no aplica. */
    private String especialidad;

    /** Subespecialidad médica; {@code null} si no aplica. */
    @JsonProperty("sub_especialidad")
    private String subEspecialidad;

    /** Cédula profesional emitida por la SEP. Puede ser {@code null} si hay documento en trámite. */
    @JsonProperty("cedula_profesional")
    private String cedulaProfesional;

    /** Cédula de especialidad médica; {@code null} si no aplica. */
    @JsonProperty("cedula_especialidad")
    private String cedulaEspecialidad;

    /** Institución que otorgó la especialidad; {@code null} si no aplica. */
    @JsonProperty("institucion_especialidad")
    private String institucionEspecialidad;

    /** URL del documento que acredita cédula en trámite. Requerido si {@code cedula_profesional} es {@code null}. */
    @JsonProperty("documento_tramite_url")
    private String documentoTramiteUrl;
}
