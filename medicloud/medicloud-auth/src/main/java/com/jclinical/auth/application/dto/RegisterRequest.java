package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para la solicitud de registro de un nuevo usuario en el sistema.
 */
@Data
public class RegisterRequest {
    /** Nombre completo del usuario. */
    @JsonProperty("full_name")
    private String fullName;
    
    /** Dirección de correo electrónico única del usuario. */
    private String email;

    /** Número telefónico de contacto del usuario. */
    private String phone;

    /** Contraseña elegida por el usuario para su cuenta. */
    private String password;
}
