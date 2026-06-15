package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link PasswordResetToken}.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    /**
     * Busca el token de restablecimiento de contraseña a partir de su hash SHA-256.
     *
     * @param tokenHash Hash SHA-256 del token original en Base64.
     * @return {@link Optional} conteniendo el token de restablecimiento de contraseña si existe.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
