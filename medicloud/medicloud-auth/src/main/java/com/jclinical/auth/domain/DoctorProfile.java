package com.jclinical.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


/**
 * Entidad JPA que representa el perfil profesional de un médico en el sistema.
 * Almacena datos como cédulas, especialidad, universidad y estado de verificación.
 */
@Entity
@Table(name = "doctor_profiles", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {
    /** Identificador único del perfil del médico. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identificador único del registro de personal de la clínica asociado (uno-a-uno). */
    @Column(name = "clinic_staff_id", nullable = false, unique = true)
    private UUID clinicStaffId;

    /** Identificador único de la clínica asociada. */
    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    /** Número de Cédula Profesional del médico (medicina general). */
    @Column(name = "cedula_profesional")
    private String cedulaProfesional;

    /** Número de Cédula de Especialidad del médico (si aplica). */
    @Column(name = "cedula_especialidad")
    private String cedulaEspecialidad;

    /** Nombre de la especialidad del médico. */
    private String especialidad;

    /** Subespecialidad del médico (si aplica). */
    @Column(name = "sub_especialidad")
    private String subEspecialidad;

    /** Nombre de la universidad o institución de donde egresó de la carrera. */
    @Column(name = "universidad_egreso", nullable = false)
    private String universidadEgreso;

    /** Año de egreso de la carrera. */
    @Column(name = "anio_egreso", nullable = false)
    private Integer anioEgreso;

    /** Nombre de la institución donde cursó la especialidad (si aplica). */
    @Column(name = "institucion_especialidad")
    private String institucionEspecialidad;

    /** URL del documento probatorio del trámite o de la cédula en la nube. */
    @Column(name = "documento_tramite_url")
    private String documentoTramiteUrl;

    /** Estado actual de verificación del perfil/cédula profesional. */
    @Column(name = "credential_status", nullable = false)
    @Builder.Default
    private DoctorCredentialStatus credentialStatus = DoctorCredentialStatus.EN_TRAMITE;

    /** Fecha y hora en la que fue verificado el perfil. */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** Identificador único del usuario administrador que verificó las credenciales. */
    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    /** Marca de tiempo de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Marca de tiempo de la última modificación del registro. */
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
