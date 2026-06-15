package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa la relación entre un usuario y una clínica.
 * Define el rol que desempeña el usuario como miembro de personal en dicha clínica.
 */
@Entity
@Table(name = "clinic_staff", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicStaff {

    /** Identificador único del registro de personal de la clínica. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identificador único de la clínica asociada. */
    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    /** Objeto de asociación perezosa (Lazy) con la entidad {@link Clinic}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    /** Objeto de asociación perezosa (Lazy) con el usuario ({@link User}) que labora en la clínica. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Rol del miembro del personal clínico (ej. ADMIN, DOCTOR). */
    @Column(nullable = false)
    private StaffRole role;

    /** Código de empleado asignado internamente por la clínica. */
    @Column(name = "employee_code")
    private String employeeCode;

    /** Marca de tiempo de la creación de la asignación de personal. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Marca de tiempo de la última actualización de los datos de personal. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
