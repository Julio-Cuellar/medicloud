package com.jclinical.auth.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico de respuesta que contiene un mensaje descriptivo de texto.
 * Utilizado para notificar el éxito de operaciones que no retornan datos específicos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    /** Mensaje de texto descriptivo del resultado. */
    private String message;
}
