package com.jclinical.shared.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Metadatos de la respuesta de la API.
 * Contiene información de diagnóstico/rastreo sobre la solicitud procesada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiMeta {
    /** Identificador único de la solicitud (request ID). */
    @JsonProperty("request_id")
    private String requestId;

    /** Marca de tiempo (timestamp) en la que se generó la respuesta. */
    private Instant timestamp;
}
