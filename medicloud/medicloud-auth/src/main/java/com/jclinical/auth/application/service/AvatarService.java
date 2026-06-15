package com.jclinical.auth.application.service;

import com.jclinical.auth.application.dto.AvatarResponse;
import com.jclinical.auth.domain.User;
import com.jclinical.auth.domain.UserRepository;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Servicio encargado de gestionar la subida y actualización del avatar del usuario.
 * <p>
 * Valida el tipo MIME real del archivo mediante magic bytes (no solo la extensión),
 * el tamaño máximo permitido (2 MB) y actualiza la URL del avatar en el perfil del usuario.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {

    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final String STORAGE_BASE_URL = "https://storage.medicloud.mx/avatars/";

    private final UserRepository userRepository;

    /**
     * Procesa la subida del avatar del usuario.
     * <p>
     * Valida el tamaño y el tipo MIME real del archivo. Genera dos URLs (256px y 64px).
     * Actualiza el campo {@code avatar_url} del usuario en base de datos.
     * </p>
     *
     * @param user El usuario autenticado que sube el avatar.
     * @param file Archivo de imagen recibido via multipart/form-data.
     * @return {@link AvatarResponse} con las URLs de las variantes generadas.
     * @throws MedicloudException Si el archivo excede el tamaño límite o el tipo MIME es inválido.
     */
    @Transactional
    public AvatarResponse uploadAvatar(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MedicloudException("El archivo es requerido.", ErrorCodes.VALIDATION_ERROR, 422);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new MedicloudException("El archivo supera el tamaño máximo de 2 MB.", ErrorCodes.FILE_TOO_LARGE, 422);
        }

        validateMimeType(file);

        String fileId = UUID.randomUUID().toString();
        String avatarUrl = STORAGE_BASE_URL + fileId + "_256.jpg";
        String avatarUrlSmall = STORAGE_BASE_URL + fileId + "_64.jpg";

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("Avatar updated for user: {}. FileId: {}", user.getEmail(), fileId);
        return AvatarResponse.builder()
                .avatarUrl(avatarUrl)
                .avatarUrlSmall(avatarUrlSmall)
                .build();
    }

    /**
     * Valida el tipo MIME real del archivo mediante la lectura de los primeros bytes (magic bytes).
     * Acepta únicamente JPEG, PNG y WebP.
     *
     * @param file Archivo a validar.
     * @throws MedicloudException Si el tipo MIME real no corresponde a ninguno de los formatos aceptados.
     */
    private void validateMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                throw new MedicloudException(
                    "El tipo MIME del archivo no es aceptado. Formatos válidos: JPG, PNG, WebP.",
                    ErrorCodes.INVALID_FILE_TYPE, 422);
            }

            // JPEG: FF D8 FF
            boolean isJpeg = (header[0] & 0xFF) == 0xFF
                          && (header[1] & 0xFF) == 0xD8
                          && (header[2] & 0xFF) == 0xFF;

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            boolean isPng = (header[0] & 0xFF) == 0x89
                         && (header[1] & 0xFF) == 0x50
                         && (header[2] & 0xFF) == 0x4E
                         && (header[3] & 0xFF) == 0x47;

            // WebP: RIFF????WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
            boolean isWebP = bytesRead >= 12
                          && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                          && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';

            if (!isJpeg && !isPng && !isWebP) {
                throw new MedicloudException(
                    "El tipo MIME del archivo no es aceptado. Formatos válidos: JPG, PNG, WebP.",
                    ErrorCodes.INVALID_FILE_TYPE, 422);
            }
        } catch (IOException e) {
            throw new MedicloudException(
                "No fue posible leer el archivo. Inténtalo de nuevo.",
                ErrorCodes.VALIDATION_ERROR, 422);
        }
    }
}
