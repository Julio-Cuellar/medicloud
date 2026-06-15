package com.jclinical.auth.application.helper;

import com.jclinical.auth.domain.PasswordHistory;
import com.jclinical.auth.domain.PasswordHistoryRepository;
import com.jclinical.auth.domain.User;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Componente de ayuda para la validación de contraseñas.
 * <p>
 * Verifica que las contraseñas cumplan con las reglas de complejidad y fortaleza,
 * y que no coincidan con el historial reciente de contraseñas del usuario.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class PasswordValidator {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Valida la fortaleza de una contraseña.
     * La contraseña debe tener al menos 10 caracteres y contener mayúsculas, minúsculas y números.
     *
     * @param password Contraseña en texto plano a validar.
     * @throws MedicloudException Si la contraseña es nula, muy corta o no cumple con la complejidad requerida.
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 10) {
            throw new MedicloudException("La contraseña no cumple la política del sistema.", ErrorCodes.PASSWORD_TOO_WEAK, 422);
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new MedicloudException("La contraseña no cumple la política del sistema.", ErrorCodes.PASSWORD_TOO_WEAK, 422);
        }
    }

    /**
     * Valida que la nueva contraseña no haya sido utilizada en el historial reciente del usuario.
     * Compara la nueva contraseña con el hash de las últimas contraseñas almacenadas en el historial.
     *
     * @param user        El usuario que solicita cambiar o establecer la contraseña.
     * @param newPassword Nueva contraseña en texto plano.
     * @throws MedicloudException Si la contraseña coincide con alguna de las usadas recientemente.
     */
    public void validatePasswordHistory(User user, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository.findFirst5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                throw new MedicloudException("La contraseña coincide con una de las últimas 3 usadas.", ErrorCodes.PASSWORD_RECENTLY_USED, 422);
            }
        }
    }
}
