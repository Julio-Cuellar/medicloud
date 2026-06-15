package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


/**
 * Entidad JPA que representa un token para el restablecimiento de contraseña de un usuario.
 */
@Entity
@Table(name = "password_reset_tokens", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    /** Identificador único del token de restablecimiento. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Usuario asociado al que se le restablecerá la contraseña. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash SHA-256 del token original en Base64. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Fecha y hora en la que expira la validez del token. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Fecha y hora en la que el token fue consumido/utilizado. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** Dirección IP desde la cual se solicitó el restablecimiento. */
    @Column(name = "requested_ip", length = 45)
    private String requestedIp;

    /** Marca de tiempo de creación del token. */
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
