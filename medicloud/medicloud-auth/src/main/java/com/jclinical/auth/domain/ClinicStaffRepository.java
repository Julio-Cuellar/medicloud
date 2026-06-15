package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link ClinicStaff}.
 */
@Repository
public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, UUID> {
    
    /**
     * Busca la lista de asignaciones de personal clínico asociadas a un usuario en particular.
     *
     * @param userId Identificador único del usuario.
     * @return Lista de relaciones {@link ClinicStaff} del usuario.
     */
    List<ClinicStaff> findByUserId(UUID userId);
}
