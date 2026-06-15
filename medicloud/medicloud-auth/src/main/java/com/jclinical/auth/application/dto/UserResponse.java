package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO que representa la respuesta detallada de la información del usuario.
 * Utilizado para exponer el perfil de usuario a través de la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    /** Identificador único del usuario. */
    private UUID id;
    
    /** Nombre completo del usuario. */
    @JsonProperty("full_name")
    private String fullName;
    
    /** Correo electrónico único del usuario. */
    private String email;

    /** Número telefónico de contacto del usuario. */
    private String phone;
    
    /** URL de la imagen de avatar del usuario. */
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    /** Indica si el correo electrónico ha sido verificado. */
    @JsonProperty("email_verified")
    private boolean emailVerified;
    
    /** Preferencia de tema visual de la interfaz. */
    @JsonProperty("theme_preference")
    private String themePreference;
    
    /** Indica si la cuenta del usuario está activa. */
    @JsonProperty("is_active")
    private boolean isActive;
    
    /** Marca de tiempo del último inicio de sesión del usuario. */
    @JsonProperty("last_login_at")
    private Instant lastLoginAt;
    
    /** Marca de tiempo de la creación de la cuenta de usuario. */
    @JsonProperty("created_at")
    private Instant createdAt;
    
    /** Lista de clínicas asociadas al usuario con sus respectivos roles y estados. */
    private List<UserClinicDto> clinics;
}
