package com.jclinical.auth.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jclinical.shared.domain.MedicloudException;
import com.jclinical.shared.domain.ValidationException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador de asesoramiento de controladores REST (Controller Advice) encargado de capturar
 * y formatear todas las excepciones no controladas lanzadas por la aplicación.
 * <p>
 * Genera respuestas estandarizadas con un sobre de error consistente para el cliente.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura las excepciones personalizadas del negocio ({@link MedicloudException}).
     * Retorna una respuesta con el estado HTTP de la excepción y la estructura estandarizada de error.
     *
     * @param ex La excepción {@link MedicloudException} capturada.
     * @return {@link ResponseEntity} conteniendo el sobre de error {@link ErrorEnvelope}.
     */
    @ExceptionHandler(MedicloudException.class)
    public ResponseEntity<ErrorEnvelope> handleMedicloudException(MedicloudException ex) {
        log.error("MedicloudException: {} - {}", ex.getErrorCode(), ex.getMessage());
        String requestId = UUID.randomUUID().toString();
        
        ErrorResponse errorBody = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .requestId(requestId)
                .build();
                
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorEnvelope.builder().error(errorBody).build());
    }

    /**
     * Captura las excepciones de validación de negocio o DTOs ({@link ValidationException}).
     * Mapea y retorna un desglose detallado de los errores por cada campo validado.
     *
     * @param ex La excepción {@link ValidationException} capturada.
     * @return {@link ResponseEntity} conteniendo el sobre de error {@link ErrorEnvelope} con los detalles de cada campo.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleValidationException(ValidationException ex) {
        log.error("ValidationException: {}", ex.getMessage());
        String requestId = UUID.randomUUID().toString();
        
        List<ErrorDetail> details = new ArrayList<>();
        for (Map.Entry<String, String> entry : ex.getErrors().entrySet()) {
            details.add(ErrorDetail.builder()
                    .field(entry.getKey())
                    .message(entry.getValue())
                    .build());
        }

        ErrorResponse errorBody = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(details.isEmpty() ? null : details)
                .requestId(requestId)
                .build();
                
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorEnvelope.builder().error(errorBody).build());
    }

    /**
     * Captura cualquier otra excepción no controlada en el sistema (ej. NullPointerException).
     * Retorna un estado HTTP 500 (Internal Server Error) y un mensaje genérico.
     *
     * @param ex La excepción general capturada.
     * @return {@link ResponseEntity} conteniendo el sobre de error {@link ErrorEnvelope}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleGeneralException(Exception ex) {
        log.error("GeneralException: ", ex);
        String requestId = UUID.randomUUID().toString();
        
        ErrorResponse errorBody = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("Ocurrió un error inesperado en el servidor.")
                .requestId(requestId)
                .build();
                
        return ResponseEntity.status(500)
                .body(ErrorEnvelope.builder().error(errorBody).build());
    }

    /**
     * Envoltura o sobre de error principal devuelto por la API.
     */
    @Data
    @Builder
    public static class ErrorEnvelope {
        /** Estructura detallada del cuerpo del error. */
        private ErrorResponse error;
    }

    /**
     * Estructura detallada de la respuesta de error de la API.
     */
    @Data
    @Builder
    public static class ErrorResponse {
        /** Código de error único identificativo de negocio. */
        private String code;

        /** Mensaje descriptivo legible del error. */
        private String message;

        /** Desglose detallado del error por campos individuales (opcional). */
        private List<ErrorDetail> details;

        /** Identificador de la solicitud (Request ID) para fines de rastreo y soporte. */
        @JsonProperty("request_id")
        private String requestId;
    }

    /**
     * Representación del detalle de error asociado a un campo particular del DTO de entrada.
     */
    @Data
    @Builder
    public static class ErrorDetail {
        /** Nombre del campo que falló la validación. */
        private String field;

        /** Mensaje indicando la causa específica del fallo en el campo. */
        private String message;
    }
}
