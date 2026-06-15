package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link PasswordHistory}.
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {
    
    /**
     * Recupera el historial de las últimas 5 contraseñas utilizadas por el usuario ordenadas de forma descendente por fecha de creación.
     *
     * @param userId Identificador único del usuario.
     * @return Lista de contraseñas históricas del usuario.
     */
    List<PasswordHistory> findFirst5ByUserIdOrderByCreatedAtDesc(UUID userId);
}
