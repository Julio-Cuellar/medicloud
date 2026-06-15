package com.jclinical.auth.application.service;

import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de la consulta y actualización de la información de perfil del usuario.
 * <p>
 * Construye las respuestas del perfil del usuario agregando la información de las clínicas
 * en las cuales trabaja y los roles correspondientes traducidos a su etiqueta legible.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ClinicStaffRepository clinicStaffRepository;

    /**
     * Obtiene el perfil de un usuario mapeando sus datos y la lista de clínicas asociadas.
     *
     * @param user Usuario del cual se desea consultar el perfil.
     * @return {@link UserResponse} conteniendo la información detallada del usuario y sus clínicas.
     */
    public UserResponse getProfile(User user) {
        List<ClinicStaff> staffList = clinicStaffRepository.findByUserId(user.getId());
        List<UserClinicDto> clinics = staffList.stream()
                .map(cs -> UserClinicDto.builder()
                        .id(cs.getClinicId())
                        .name(cs.getClinic() != null ? cs.getClinic().getName() : "Clínica")
                        .role(cs.getRole().getValue())
                        .roleLabel(getRoleLabel(cs.getRole()))
                        .isActive(cs.getClinic() != null && cs.getClinic().isActive())
                        .build())
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .themePreference(user.getThemePreference())
                .isActive(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .clinics(clinics)
                .build();
    }

    /**
     * Actualiza la información básica del perfil del usuario (nombre, teléfono, tema visual).
     *
     * @param user    Usuario cuyo perfil se va a actualizar.
     * @param request DTO con los datos a modificar.
     * @return {@link UserResponse} con el perfil actualizado.
     */
    @Transactional
    public UserResponse updateProfile(User user, UpdateUserRequest request) {
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getThemePreference() != null) {
            user.setThemePreference(request.getThemePreference().trim());
        }
        userRepository.save(user);
        return getProfile(user);
    }

    /**
     * Traduce el enum {@link StaffRole} a su etiqueta legible correspondiente en español.
     *
     * @param role Enum del rol del personal.
     * @return Etiqueta legible del rol.
     */
    private String getRoleLabel(StaffRole role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case DOCTOR -> "Médico";
            case RECEPTIONIST -> "Recepcionista";
            case ASSISTANT -> "Asistente";
            case ACCOUNTANT -> "Contador";
            case CLEANING -> "Personal de limpieza";
            case CLINIC_ADMIN -> "Administrador de Clínica";
        };
    }
}
