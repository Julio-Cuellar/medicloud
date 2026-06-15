package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para la solicitud de aceptación de una invitación de personal.
 * Contiene el token de la invitación y la contraseña elegida por el usuario.
 */
@Data
public class AcceptInvitationRequest {
    /** Token de la invitación enviada al correo del miembro del personal. */
    @JsonProperty("invitation_token")
    private String invitationToken;
    
    /** Contraseña elegida por el miembro del personal para su cuenta. */
    private String password;
}
