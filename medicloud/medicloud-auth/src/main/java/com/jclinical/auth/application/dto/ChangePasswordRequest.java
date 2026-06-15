package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para la solicitud de cambio de contraseña para un usuario autenticado.
 */
@Data
public class ChangePasswordRequest {
    /** Contraseña actual del usuario. */
    @JsonProperty("current_password")
    private String currentPassword;
    
    /** Nueva contraseña deseada por el usuario. */
    @JsonProperty("new_password")
    private String newPassword;
}
