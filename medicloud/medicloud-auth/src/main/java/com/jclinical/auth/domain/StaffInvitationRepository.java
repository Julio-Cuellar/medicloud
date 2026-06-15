package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link StaffInvitation}.
 */
@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {
    
    /**
     * Busca la invitación de personal a partir del hash del token.
     *
     * @param tokenHash Hash SHA-256 del token de invitación original en Base64.
     * @return {@link Optional} conteniendo la invitación de personal si existe.
     */
    Optional<StaffInvitation> findByTokenHash(String tokenHash);
}
