package com.jclinical.shared.domain;

import lombok.Getter;

/**
 * Excepción base personalizada para la plataforma Medicloud.
 * Contiene un código de error de negocio y el estado HTTP asociado.
 */
@Getter
public class MedicloudException extends RuntimeException {
    /** Código de error específico de la aplicación (definido en {@link ErrorCodes}). */
    private final String errorCode;

    /** Código de estado HTTP recomendado para la respuesta. */
    private final int httpStatus;

    /**
     * Construye una nueva excepción MedicloudException.
     *
     * @param message    El mensaje explicativo del error.
     * @param errorCode  El código de error específico del negocio.
     * @param httpStatus El estado HTTP asociado a este error.
     */
    public MedicloudException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
