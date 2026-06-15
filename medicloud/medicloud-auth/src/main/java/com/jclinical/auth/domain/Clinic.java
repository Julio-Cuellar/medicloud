package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "clinics", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
