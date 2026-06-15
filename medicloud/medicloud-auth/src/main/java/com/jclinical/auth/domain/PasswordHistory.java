package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_history", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
