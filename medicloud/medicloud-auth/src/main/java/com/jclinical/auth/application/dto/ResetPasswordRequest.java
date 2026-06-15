package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para la solicitud de restablecimiento de contraseña utilizando un token.
 */
@Data
public class ResetPasswordRequest {
    /** Token seguro enviado previamente al correo electrónico del usuario. */
    private String token;
    
    /** Nueva contraseña a establecer para la cuenta. */
    @JsonProperty("new_password")
    private String newPassword;
}
