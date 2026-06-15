package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa un token para la verificación del correo electrónico de un usuario.
 */
@Entity
@Table(name = "email_verification_tokens", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {
    /** Identificador único del token de verificación. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Usuario asociado al token de verificación de correo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash SHA-256 del token original enviado por correo. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Fecha y hora en la que expira la validez del token. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Fecha y hora en la que el token fue consumido/utilizado. */
    @Column(name = "used_at")
    private Instant usedAt;

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
