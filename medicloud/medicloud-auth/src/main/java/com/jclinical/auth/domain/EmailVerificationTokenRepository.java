package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link EmailVerificationToken}.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    
    /**
     * Busca el token de verificación de correo a partir del hash del token.
     *
     * @param tokenHash Hash SHA-256 del token original en Base64.
     * @return {@link Optional} conteniendo el token de verificación si existe.
     */
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
