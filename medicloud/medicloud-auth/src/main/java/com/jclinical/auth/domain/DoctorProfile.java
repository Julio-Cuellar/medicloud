package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "doctor_profiles", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_staff_id", nullable = false, unique = true)
    private UUID clinicStaffId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "cedula_profesional")
    private String cedulaProfesional;

    @Column(name = "cedula_especialidad")
    private String cedulaEspecialidad;

    private String especialidad;

    @Column(name = "sub_especialidad")
    private String subEspecialidad;

    @Column(name = "universidad_egreso", nullable = false)
    private String universidadEgreso;

    @Column(name = "anio_egreso", nullable = false)
    private Integer anioEgreso;

    @Column(name = "institucion_especialidad")
    private String institucionEspecialidad;

    @Column(name = "documento_tramite_url")
    private String documentoTramiteUrl;

    @Column(name = "credential_status", nullable = false)
    @Builder.Default
    private DoctorCredentialStatus credentialStatus = DoctorCredentialStatus.EN_TRAMITE;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
