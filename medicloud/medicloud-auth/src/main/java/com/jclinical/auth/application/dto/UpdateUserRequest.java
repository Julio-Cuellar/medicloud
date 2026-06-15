package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para la solicitud de actualización de perfil de usuario.
 */
@Data
public class UpdateUserRequest {
    /** Nuevo nombre completo del usuario. */
    @JsonProperty("full_name")
    private String fullName;
    
    /** Nuevo número telefónico de contacto del usuario. */
    private String phone;
    
    /** Nueva preferencia de tema de la interfaz (ej. "light", "dark"). */
    @JsonProperty("theme_preference")
    private String themePreference;
}
