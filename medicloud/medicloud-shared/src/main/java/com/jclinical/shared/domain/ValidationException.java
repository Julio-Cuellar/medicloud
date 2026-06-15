package com.jclinical.shared.domain;

import lombok.Getter;
import java.util.Map;
import java.util.Collections;

/**
 * Excepción lanzada cuando falla la validación de negocio o de datos de entrada.
 * Retorna automáticamente un estado HTTP 422 (Unprocessable Entity) y
 * puede contener un mapa detallado con los errores por campo.
 */
@Getter
public class ValidationException extends MedicloudException {
    /** Mapa que asocia cada campo inválido con su mensaje de error correspondiente. */
    private final Map<String, String> errors;

    /**
     * Construye una nueva excepción ValidationException con un mensaje genérico.
     *
     * @param message Mensaje que describe el fallo general de validación.
     */
    public ValidationException(String message) {
        super(message, ErrorCodes.VALIDATION_ERROR, 422);
        this.errors = Collections.emptyMap();
    }

    /**
     * Construye una nueva excepción ValidationException con un mensaje genérico
     * y un mapa detallado de errores por campo.
     *
     * @param message Mensaje que describe el fallo general de validación.
     * @param errors  Mapa de errores por campo (clave: campo, valor: descripción del error).
     */
    public ValidationException(String message, Map<String, String> errors) {
        super(message, ErrorCodes.VALIDATION_ERROR, 422);
        this.errors = errors != null ? errors : Collections.emptyMap();
    }
}
