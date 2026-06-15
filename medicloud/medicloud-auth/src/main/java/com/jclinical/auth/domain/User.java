package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


/**
 * Entidad JPA que representa la información de la cuenta de un usuario en el sistema.
 * Contiene metadatos de seguridad, preferencias y marcas de tiempo del ciclo de vida de la cuenta.
 */
@Entity
@Table(name = "users", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Identificador único del usuario. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Dirección de correo electrónico única del usuario. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Número telefónico de contacto del usuario. */
    private String phone;

    /** Nombre completo del usuario. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Hash bcrypt de la contraseña del usuario. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** URL de la imagen de avatar del usuario. */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Indica si la dirección de correo electrónico del usuario ha sido verificada. */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Preferencia de tema visual de la interfaz del usuario. */
    @Column(name = "theme_preference", nullable = false)
    @Builder.Default
    private String themePreference = "light";

    /** Número de intentos fallidos consecutivos de inicio de sesión. */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    /** Marca de tiempo hasta la cual la cuenta se encuentra bloqueada por intentos fallidos. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Marca de tiempo del último inicio de sesión exitoso. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Marca de tiempo en la que se creó el registro del usuario. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Marca de tiempo de la última modificación del registro del usuario. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Indica si el usuario está activo en la plataforma. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Hook JPA ejecutado antes de persistir la entidad para establecer los tiempos de creación y modificación.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /**
     * Hook JPA ejecutado antes de actualizar la entidad para refrescar el tiempo de modificación.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
