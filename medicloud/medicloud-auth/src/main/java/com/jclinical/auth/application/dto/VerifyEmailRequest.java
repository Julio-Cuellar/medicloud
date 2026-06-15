package com.jclinical.auth.application.dto;

import lombok.Data;

/**
 * DTO para la solicitud de verificación de correo electrónico.
 */
@Data
public class VerifyEmailRequest {
    /** Token de verificación seguro que se envió por correo al usuario. */
    private String token;
}
