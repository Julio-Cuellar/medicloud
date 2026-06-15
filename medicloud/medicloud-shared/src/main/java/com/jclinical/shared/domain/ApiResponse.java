package com.jclinical.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Estructura de respuesta genérica y unificada para todas las peticiones de la API.
 * Encapsula el cuerpo principal de la respuesta (data) y sus metadatos (meta).
 *
 * @param <T> El tipo del cuerpo de datos de la respuesta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    
    /** Los datos principales devueltos por el endpoint. */
    private T data;

    /** Metadatos asociados a la respuesta (ID de solicitud, timestamp, etc.). */
    private ApiMeta meta;

    /**
     * Genera una respuesta exitosa con un identificador único aleatorio para la solicitud.
     *
     * @param <T>  El tipo del cuerpo de datos.
     * @param data El cuerpo de datos a retornar en la respuesta.
     * @return Una instancia de {@code ApiResponse} con los datos y metadatos correspondientes.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(ApiMeta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    /**
     * Genera una respuesta exitosa asociándola a un identificador de solicitud preexistente.
     *
     * @param <T>       El tipo del cuerpo de datos.
     * @param data      El cuerpo de datos a retornar en la respuesta.
     * @param requestId El identificador de la solicitud origen.
     * @return Una instancia de {@code ApiResponse} con los datos y metadatos correspondientes.
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(ApiMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();
    }
}
