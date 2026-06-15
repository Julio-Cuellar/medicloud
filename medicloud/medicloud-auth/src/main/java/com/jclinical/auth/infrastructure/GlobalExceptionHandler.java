package com.jclinical.auth.infrastructure;

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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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

    @Data
    @Builder
    public static class ErrorEnvelope {
        private ErrorResponse error;
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private String code;
        private String message;
        private List<ErrorDetail> details;
        private String requestId;
    }

    @Data
    @Builder
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}
