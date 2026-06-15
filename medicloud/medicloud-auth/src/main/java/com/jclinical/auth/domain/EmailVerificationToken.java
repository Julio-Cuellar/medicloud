package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
