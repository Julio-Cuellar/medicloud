package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta tras verificar las credenciales profesionales de un médico.
 * Retornado por el endpoint {@code PATCH /v1/staff/{id}/credentials/verify}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyCredentialsResponse {

    /** Identificador único del miembro del staff cuya credencial fue verificada. */
    @JsonProperty("staff_id")
    private UUID staffId;

    /** Estado actualizado de la credencial profesional: {@code activo} o {@code en_tramite}. */
    @JsonProperty("credential_status")
    private String credentialStatus;

    /** Fecha y hora en que se realizó la verificación. */
    @JsonProperty("verified_at")
    private Instant verifiedAt;
}
