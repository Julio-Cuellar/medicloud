package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link RefreshToken}.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Busca un Refresh Token a partir de su hash SHA-256.
     *
     * @param tokenHash Hash SHA-256 del token original en Base64.
     * @return {@link Optional} conteniendo el Refresh Token correspondiente si existe.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Elimina todos los Refresh Tokens asociados a un usuario en particular (cierre de sesión global).
     *
     * @param userId Identificador único del usuario.
     */
    void deleteByUserId(UUID userId);
}
