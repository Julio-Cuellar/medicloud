package com.jclinical.auth.infrastructure;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Clase de configuración para la criptografía y decodificación de tokens JWT.
 * <p>
 * Genera dinámicamente un par de claves RSA de 2048 bits para firmar y verificar tokens,
 * y expone los beans de NimbusJwtEncoder y NimbusJwtDecoder para la seguridad de la API.
 * </p>
 */
@Configuration
public class AuthorizationServerConfig {

    /** Par de claves criptográficas (pública/privada) utilizadas para firmar/verificar JWTs. */
    private final KeyPair keyPair;

    /**
     * Constructor que inicializa el par de claves RSA de 2048 bits de forma predeterminada al arrancar la aplicación.
     *
     * @throws IllegalStateException Si no se encuentra disponible el algoritmo RSA en el entorno.
     */
    public AuthorizationServerConfig() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            this.keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Define la fuente de claves JWK (JSON Web Key) utilizando el par de claves RSA generado.
     *
     * @return El bean {@link JWKSource} con el conjunto de claves JWK configurado.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Define el codificador de tokens JWT utilizando la fuente JWK configurada.
     *
     * @param jwkSource Fuente de claves JWK.
     * @return El bean {@link JwtEncoder} configurado.
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Define el decodificador de tokens JWT utilizando la clave pública RSA generada.
     *
     * @return El bean {@link JwtDecoder} configurado.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }
}
