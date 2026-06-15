package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta tras la subida exitosa del avatar del usuario.
 * Contiene las URLs de las dos variantes de imagen generadas por el sistema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarResponse {

    /** URL de la imagen de avatar en resolución estándar (256×256 px). */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** URL de la imagen de avatar en resolución reducida (64×64 px). */
    @JsonProperty("avatar_url_small")
    private String avatarUrlSmall;
}
