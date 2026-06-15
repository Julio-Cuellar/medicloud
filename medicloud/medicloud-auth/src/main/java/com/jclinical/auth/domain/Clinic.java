package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Entidad JPA que representa una clínica dentro del sistema.
 */
@Entity
@Table(name = "clinics", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {
    /** Identificador único de la clínica. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nombre de la clínica. */
    @Column(nullable = false)
    private String name;

    /** Indica si la clínica se encuentra activa en el sistema. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
