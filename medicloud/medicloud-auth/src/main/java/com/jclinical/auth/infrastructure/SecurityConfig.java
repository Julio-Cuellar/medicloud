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
 * Configuración de seguridad Web y de APIs basada en Spring Security.
 * <p>
 * Define las reglas de acceso de los endpoints de la API (públicos y protegidos),
 * la política de sesiones stateless, la integración como Resource Server OAuth2
 * con JWT, y el codificador de contraseñas de la aplicación.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Cadena de filtros para los endpoints {@code /oauth2/**}: el endpoint
     * {@code POST /oauth2/token} es completamente público.
     *
     * @param http Objeto HttpSecurity de Spring.
     * @return El {@link SecurityFilterChain} configurado para /oauth2.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/oauth2/**")
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/token").permitAll()
                .anyRequest().denyAll()
            );

        return http.build();
    }

    /**
     * Cadena de filtros de seguridad para los endpoints {@code /v1/**}.
     * Define rutas públicas de autenticación y exige JWT para cualquier otra ruta.
     *
     * @param http Objeto HttpSecurity de Spring.
     * @return El {@link SecurityFilterChain} configurado para /v1.
     * @throws Exception Si ocurre un error en la configuración.
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
     * Define el PasswordEncoder para cifrar las contraseñas con BCrypt (strength 12).
     *
     * @return El {@link PasswordEncoder} configurado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
