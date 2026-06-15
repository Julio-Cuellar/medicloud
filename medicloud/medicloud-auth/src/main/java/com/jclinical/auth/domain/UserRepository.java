package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Busca un usuario por su dirección de correo electrónico única.
     *
     * @param email Correo electrónico a buscar.
     * @return {@link Optional} con el usuario encontrado o vacío si no existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica la existencia de un usuario por su dirección de correo electrónico única.
     *
     * @param email Correo electrónico a verificar.
     * @return {@code true} si existe un usuario con dicho correo, {@code false} en caso contrario.
     */
    boolean existsByEmail(String email);
}
