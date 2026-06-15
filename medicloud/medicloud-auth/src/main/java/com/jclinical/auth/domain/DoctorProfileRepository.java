package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link DoctorProfile}.
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    
    /**
     * Busca el perfil del doctor asociado a una asignación de personal clínico específica.
     *
     * @param clinicStaffId Identificador único de la asignación de personal clínico.
     * @return {@link Optional} con el perfil del doctor si existe.
     */
    Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId);
}
