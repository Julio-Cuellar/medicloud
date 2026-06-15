package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * DTO que representa la relación y el rol de un usuario con respecto a una clínica específica.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserClinicDto {
    /** Identificador de la clínica. */
    private UUID id;

    /** Nombre de la clínica. */
    private String name;

    /** Rol del usuario dentro de la clínica (ej. "ADMIN", "DOCTOR", "RECEPCIONIST"). */
    private String role;
    
    /** Etiqueta legible en español para mostrar en interfaz de usuario. */
    @JsonProperty("role_label")
    private String roleLabel;
    
    /** Estado del miembro del personal en la clínica (activo/inactivo). */
    @JsonProperty("is_active")
    private boolean isActive;
}
