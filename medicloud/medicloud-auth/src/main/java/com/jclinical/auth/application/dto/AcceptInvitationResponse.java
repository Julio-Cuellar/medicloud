package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta tras aceptar exitosamente una invitación de staff.
 * El usuario queda autenticado. El refresh token se emite como cookie HttpOnly,
 * no en el cuerpo de la respuesta.
 * Retornado por el endpoint {@code POST /v1/auth/accept-invitation}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptInvitationResponse {

    /** Token de acceso JWT para autorizar las solicitudes subsiguientes. */
    @JsonProperty("access_token")
    private String accessToken;

    /** Tipo de token (siempre {@code Bearer}). */
    @JsonProperty("token_type")
    private String tokenType;

    /** Tiempo de vida del access token en segundos. */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Token de refresco. Ignorado en la serialización JSON; se envía como cookie HttpOnly.
     */
    @JsonIgnore
    private String refreshToken;

    /** Perfil resumido del usuario recién autenticado. */
    private UserSummaryDto user;

    /** Clínica a la que el usuario se incorporó con esta invitación. */
    @JsonProperty("joined_clinic")
    private ClinicSummaryDto joinedClinic;

    /**
     * DTO con el perfil resumido del usuario (sin campos internos de seguridad).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummaryDto {

        /** Identificador único del usuario. */
        private UUID id;

        /** Nombre completo del usuario. */
        @JsonProperty("full_name")
        private String fullName;

        /** Correo electrónico del usuario. */
        private String email;

        /** Clínicas y roles asociados al usuario. */
        private List<UserClinicDto> clinics;
    }

    /**
     * DTO con el resumen de la clínica a la que el usuario se unió.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClinicSummaryDto {

        /** Identificador único de la clínica. */
        private UUID id;

        /** Nombre de la clínica. */
        private String name;
    }
}
