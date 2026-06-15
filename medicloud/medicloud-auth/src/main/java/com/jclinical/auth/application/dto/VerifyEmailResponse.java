package com.jclinical.auth.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la respuesta tras verificar exitosamente un correo electrónico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailResponse {
    /** Mensaje informativo sobre el resultado de la verificación. */
    private String message;

    /** Dirección de correo electrónico verificada. */
    private String email;
}
