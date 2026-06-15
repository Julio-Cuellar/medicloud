package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
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

    /**
     * Busca un miembro del staff por su ID dentro de una clínica específica.
     *
     * @param id       ID del registro de staff.
     * @param clinicId ID de la clínica.
     * @return El registro si existe en esa clínica.
     */
    Optional<ClinicStaff> findByIdAndClinicId(UUID id, UUID clinicId);

    /**
     * Verifica si un usuario ya pertenece a una clínica específica.
     *
     * @param userId   ID del usuario.
     * @param clinicId ID de la clínica.
     * @return {@code true} si ya existe el vínculo.
     */
    boolean existsByUserIdAndClinicId(UUID userId, UUID clinicId);

    /**
     * Busca un registro de staff de un usuario dentro de una clínica específica.
     *
     * @param userId   ID del usuario.
     * @param clinicId ID de la clínica.
     * @return El registro de staff si existe para ese usuario en esa clínica.
     */
    Optional<ClinicStaff> findByUserIdAndClinicId(UUID userId, UUID clinicId);
}
