package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


/**
 * Entidad JPA que representa un token de refresco (Refresh Token) en el esquema de autenticación (OAuth2).
 * Utilizado para renovar tokens de acceso expirados implementando rotación y revocación segura.
 */
@Entity
@Table(name = "refresh_tokens", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /** Identificador único del refresh token. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Usuario ({@link User}) al cual está asociado este token de refresco. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash SHA-256 del valor del token de refresco en Base64. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Etiqueta descriptiva del dispositivo que generó la sesión (ej. "Web App"). */
    @Column(name = "device_label")
    private String deviceLabel;

    /** Dirección IP desde la cual se realizó el inicio de sesión o renovación. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Cadena de agente de usuario del navegador o cliente origen. */
    @Column(name = "user_agent")
    private String userAgent;

    /** Fecha y hora en la que expira la validez del token de refresco (típicamente 30 días). */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Fecha y hora en la que el token de refresco se utilizó por última vez. */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Fecha y hora en la que el token fue revocado explícita o implícitamente por rotación. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Identificador del token de refresco que reemplazó a este tras la rotación exitosa. */
    @Column(name = "replaced_by_id")
    private UUID replacedById;

    /** Marca de tiempo de la creación del token de refresco. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Hook JPA ejecutado antes de persistir la entidad para establecer la fecha de creación.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
