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

@Component
@RequiredArgsConstructor
public class PasswordValidator {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

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

    public void validatePasswordHistory(User user, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository.findFirst5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                throw new MedicloudException("La contraseña coincide con una de las últimas 3 usadas.", ErrorCodes.PASSWORD_RECENTLY_USED, 422);
            }
        }
    }
}
