package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Elimina todos los refresh tokens de un usuario excepto el de la sesión activa actual.
     * Usado en cambio de contraseña para no cerrar la sesión del dispositivo que realizó el cambio.
     *
     * @param userId          ID del usuario.
     * @param currentTokenId  ID del token de la sesión activa a preservar.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.id <> :currentTokenId")
    void deleteByUserIdExcept(@Param("userId") UUID userId, @Param("currentTokenId") UUID currentTokenId);
}
