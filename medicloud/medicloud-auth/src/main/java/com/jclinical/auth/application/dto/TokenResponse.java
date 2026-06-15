package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que encapsula la respuesta tras una autenticación exitosa.
 * Contiene los tokens de acceso y la información del usuario autenticado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    /** Token de acceso JWT utilizado para autorizar las solicitudes subsiguientes. */
    @JsonProperty("access_token")
    private String accessToken;

    /** Tipo de token provisto (por ejemplo, "Bearer"). */
    @JsonProperty("token_type")
    private String tokenType;

    /** Tiempo de vida restante del token de acceso en segundos. */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Token de refresco utilizado para renovar el token de acceso expirado.
     * Este campo está ignorado en la serialización JSON directa por motivos de seguridad
     * (ya que se envía habitualmente a través de una cookie HttpOnly).
     */
    @JsonIgnore
    private String refreshToken;

    /** Información del perfil del usuario autenticado. */
    private UserResponse user;
}
