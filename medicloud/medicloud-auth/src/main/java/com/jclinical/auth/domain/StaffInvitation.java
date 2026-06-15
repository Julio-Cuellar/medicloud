package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_invitations", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private StaffRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
