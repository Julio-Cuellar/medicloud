package com.jclinical.auth.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Clase de configuración para la seguridad Web y de APIs basada en Spring Security.
 * <p>
 * Define las reglas de acceso de los endpoints de la API (públicos y protegidos),
 * la política de sesiones stateless (sin estado) para APIs REST, la integración
 * como servidor de recursos OAuth2 (Resource Server) usando tokens JWT, y
 * el codificador de contraseñas de la aplicación.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configura la cadena de filtros de seguridad para los endpoints que comienzan con {@code /v1/**}.
     * Deshabilita CSRF, habilita CORS por defecto, establece la sesión como STATELESS y
     * define las rutas públicas de autenticación permitiendo el paso libre, mientras que
     * exige autenticación JWT para cualquier otra ruta.
     *
     * @param http Objeto HttpSecurity para configurar la seguridad web.
     * @return El filtro de seguridad {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre algún error en la configuración.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/v1/**")
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/v1/auth/token",
                    "/v1/auth/register",
                    "/v1/auth/verify-email",
                    "/v1/auth/resend-verification",
                    "/v1/auth/request-password-reset",
                    "/v1/auth/reset-password",
                    "/v1/auth/accept-invitation"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }

    /**
     * Define el PasswordEncoder para cifrar de forma segura las contraseñas de los usuarios.
     * Utiliza el algoritmo BCrypt con una fuerza de hash (strength/work factor) establecida en 12.
     *
     * @return El codificador {@link PasswordEncoder} configurado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
