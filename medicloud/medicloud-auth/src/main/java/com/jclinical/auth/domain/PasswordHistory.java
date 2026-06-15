package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que almacena el historial de contraseñas de los usuarios.
 * Permite validar que el usuario no reutilice contraseñas recientes.
 */
@Entity
@Table(name = "password_history", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistory {
    /** Identificador único del registro de contraseña histórica. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Usuario al que le pertenece esta contraseña del historial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash bcrypt de la contraseña histórica. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Marca de tiempo de la creación del registro histórico. */
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
