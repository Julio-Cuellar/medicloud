package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa una invitación enviada a un miembro potencial del personal clínico o administrativo.
 */
@Entity
@Table(name = "staff_invitations", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitation {
    /** Identificador único de la invitación. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identificador único de la clínica a la cual se invita al personal. */
    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    /** Asociación perezosa (Lazy) con la clínica ({@link Clinic}) de destino de la invitación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    /** Dirección de correo electrónico del destinatario de la invitación. */
    @Column(nullable = false)
    private String email;

    /** Rol que se le asignará al destinatario al aceptar la invitación (ej. DOCTOR, RECEPTIONIST). */
    @Column(nullable = false)
    private StaffRole role;

    /** Usuario administrador ({@link User}) que generó y envió la invitación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    /** Hash SHA-256 del token de invitación único enviado por correo. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Estado actual de la invitación (ej. PENDING, ACCEPTED, EXPIRED, REVOKED). */
    @Column(nullable = false)
    private InvitationStatus status;

    /** Fecha y hora de vencimiento de la invitación. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Fecha y hora en la que la invitación fue aceptada por el usuario. */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /** Usuario ({@link User}) creado o asociado cuando se aceptó la invitación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    /** Marca de tiempo de la creación del registro de invitación. */
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
