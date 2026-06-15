package com.jclinical.shared.domain;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 * Retorna automáticamente un estado HTTP 404 (Not Found).
 */
public class ResourceNotFoundException extends MedicloudException {
    
    /**
     * Construye una nueva excepción ResourceNotFoundException con el mensaje especificado.
     *
     * @param message Mensaje detallado del recurso que no se encontró.
     */
    public ResourceNotFoundException(String message) {
        super(message, ErrorCodes.RESOURCE_NOT_FOUND, 404);
    }
}
