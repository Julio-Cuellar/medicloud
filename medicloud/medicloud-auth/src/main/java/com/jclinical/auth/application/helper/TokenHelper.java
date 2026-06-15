package com.jclinical.auth.application.helper;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Clase de ayuda para la generación y el hash seguro de tokens.
 * <p>
 * Proporciona métodos para generar tokens aleatorios seguros y calcular hashes SHA-256 de los mismos.
 * </p>
 */
@Component
public class TokenHelper {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un token aleatorio seguro de 32 bytes codificado en Base64 URL-safe sin padding.
     *
     * @return Cadena de texto que representa el token aleatorio generado.
     */
    public String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calcula el hash SHA-256 de un token en formato hexadecimal.
     * Utilizado para almacenar de forma segura tokens en la base de datos.
     *
     * @param token Token original en texto plano.
     * @return Cadena hexadecimal de 64 caracteres representando el hash del token, o {@code null} si el token provisto es nulo.
     * @throws IllegalStateException Si ocurre un error al inicializar el algoritmo de hash SHA-256.
     */
    public String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
