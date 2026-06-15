package com.jclinical.auth.application.dto;

import lombok.Data;

/**
 * DTO para la solicitud de reenvío de correo de verificación de cuenta
 * o para solicitar el restablecimiento de contraseña.
 */
@Data
public class ResendVerificationRequest {
    /** Dirección de correo electrónico del usuario destinatario. */
    private String email;
}
