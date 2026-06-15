package com.jclinical.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.auth.application.dto.*;
import com.jclinical.auth.application.helper.PasswordValidator;
import com.jclinical.auth.application.helper.TokenHelper;
import com.jclinical.auth.domain.*;
import com.jclinical.shared.domain.ErrorCodes;
import com.jclinical.shared.domain.MedicloudException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de gestionar el ciclo de vida del personal (staff) de una clínica.
 * <p>
 * Cubre el flujo de invitación ({@code POST /v1/staff/invite}),
 * la aceptación de invitación ({@code POST /v1/auth/accept-invitation}),
 * la consulta de perfil de staff ({@code GET /v1/staff/{id}}) y
 * la verificación de credenciales de doctor ({@code PATCH /v1/staff/{id}/credentials/verify}).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final UserRepository userRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final StaffInvitationRepository staffInvitationRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenHelper tokenHelper;
    private final JwtEncoder jwtEncoder;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    /**
     * Envía una invitación de staff por correo electrónico a un nuevo empleado de la clínica.
     * <p>
     * Si el rol asignado es {@code doctor}, valida y persiste las credenciales profesionales
     * conforme a los requisitos de la NOM-004-SSA3-2012.
     * </p>
     *
     * @param clinicId      ID de la clínica activa (tomado del header {@code X-Clinic-ID}).
     * @param invitedById   ID del usuario administrador que envía la invitación.
     * @param request       Datos del invitado y credenciales opcionales de doctor.
     * @return {@link InviteStaffResponse} con el estado del envío y las credenciales calculadas.
     * @throws MedicloudException Si el rol es {@code doctor} y no se proporcionan credenciales válidas.
     */
    @Transactional
    public InviteStaffResponse inviteStaff(UUID clinicId, UUID invitedById, InviteStaffRequest request) {
        ClinicStaff inviter = clinicStaffRepository.findByUserIdAndClinicId(invitedById, clinicId)
                .orElseThrow(() -> new MedicloudException(
                        "El usuario que invita no pertenece a la clínica activa.",
                        ErrorCodes.CLINIC_ACCESS_DENIED, 403));

        if (!StaffRole.ADMIN.equals(inviter.getRole()) && !StaffRole.CLINIC_ADMIN.equals(inviter.getRole())) {
            throw new MedicloudException(
                    "Solo un administrador puede invitar miembros al staff.",
                    ErrorCodes.FORBIDDEN, 403);
        }

        StaffRole role = parseRole(request.getRole());

        InviteStaffResponse.DoctorCredentialsResponseDto credentialsDto = null;

        if (StaffRole.DOCTOR.equals(role)) {
            credentialsDto = validateAndBuildDoctorCredentials(request.getDoctorCredentials());
        }

        User invitedBy = userRepository.findById(invitedById)
                .orElseThrow(() -> new MedicloudException("Usuario no encontrado", ErrorCodes.RESOURCE_NOT_FOUND, 404));

        String rawToken = tokenHelper.generateRandomToken();
        String tokenHash = tokenHelper.hashToken(rawToken);

        JsonNode credentialsJson = null;
        if (StaffRole.DOCTOR.equals(role) && request.getDoctorCredentials() != null) {
            credentialsJson = objectMapper.valueToTree(request.getDoctorCredentials());
        }

        StaffInvitation invitation = StaffInvitation.builder()
                .clinicId(clinicId)
                .email(request.getEmail().toLowerCase().trim())
                .fullName(request.getFullName())
                .role(role)
                .invitedBy(invitedBy)
                .tokenHash(tokenHash)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(72, ChronoUnit.HOURS))
                .doctorCredentialsJson(credentialsJson)
                .build();

        staffInvitationRepository.save(invitation);

        log.info("Staff invitation sent. Email: {}, Role: {}, ClinicId: {}, Token: {}",
                request.getEmail(), role, clinicId, rawToken);

        return InviteStaffResponse.builder()
                .invitationSent(true)
                .email(request.getEmail())
                .role(request.getRole())
                .doctorCredentials(credentialsDto)
                .build();
    }

    /**
     * Acepta una invitación de staff y autentica al usuario resultante.
     * <p>
     * <b>Escenario A (usuario nuevo):</b> El email invitado no existe en el sistema.
     * Se crea el usuario y se requiere contraseña en el request.
     * </p>
     * <p>
     * <b>Escenario B (usuario existente):</b> El email ya tiene cuenta activa.
     * Solo se vincula la clínica; no se solicita contraseña.
     * </p>
     *
     * @param request Datos de la invitación: token e, opcionalmente, contraseña.
     * @return {@link AcceptInvitationResponse} con access_token, refresh_token (para cookie) y perfil.
     * @throws MedicloudException Si el token es inválido, expirado, o faltan datos requeridos.
     */
    @Transactional
    public AcceptInvitationResponse acceptInvitation(AcceptInvitationRequest request) {
        String tokenHash = tokenHelper.hashToken(request.getInvitationToken());
        StaffInvitation invitation = staffInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new MedicloudException(
                        "Token de invitación inválido, ya utilizado o sin clínica válida.",
                        ErrorCodes.INVITATION_TOKEN_INVALID, 401));

        if (!InvitationStatus.PENDING.equals(invitation.getStatus())) {
            throw new MedicloudException(
                    "Token de invitación inválido, ya utilizado o sin clínica válida.",
                    ErrorCodes.INVITATION_TOKEN_INVALID, 401);
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new MedicloudException(
                    "Token de invitación vencido (más de 72 horas).",
                    ErrorCodes.INVITATION_TOKEN_EXPIRED, 401);
        }

        String email = invitation.getEmail();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Escenario A: usuario nuevo — se requiere contraseña
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new MedicloudException(
                        "El campo 'password' es obligatorio para nuevos usuarios.",
                        ErrorCodes.PASSWORD_REQUIRED, 422);
            }
            passwordValidator.validatePasswordStrength(request.getPassword());

            user = User.builder()
                    .email(email)
                    .fullName(invitation.getFullName() != null ? invitation.getFullName() : email)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .emailVerified(true)
                    .themePreference("light")
                    .isActive(true)
                    .build();
            userRepository.save(user);
            log.info("New user created via invitation: {}", email);
        } else {
            // Escenario B: usuario existente — solo se vincula
            log.info("Existing user accepting invitation: {}", email);
        }

        // Vincular el usuario a la clínica si aún no pertenece
        if (!clinicStaffRepository.existsByUserIdAndClinicId(user.getId(), invitation.getClinicId())) {
            ClinicStaff clinicStaff = ClinicStaff.builder()
                    .clinicId(invitation.getClinicId())
                    .user(user)
                    .role(invitation.getRole())
                    .build();
            clinicStaff = clinicStaffRepository.save(clinicStaff);

            if (StaffRole.DOCTOR.equals(invitation.getRole())
                    && invitation.getDoctorCredentialsJson() != null) {
                createDoctorProfile(clinicStaff, invitation);
            }
        }

        // Marcar invitación como aceptada
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setAcceptedUser(user);
        staffInvitationRepository.save(invitation);

        // Generar tokens de sesión
        return generateAcceptInvitationResponse(user, invitation);
    }

    /**
     * Obtiene el perfil detallado de un miembro del staff de una clínica específica.
     *
     * @param clinicId ID de la clínica activa (del header {@code X-Clinic-ID}).
     * @param staffId  ID del miembro del staff a consultar.
     * @return {@link StaffResponse} con el perfil completo y credenciales si es doctor.
     * @throws MedicloudException Si el miembro no existe en la clínica indicada.
     */
    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID clinicId, UUID staffId) {
        ClinicStaff cs = clinicStaffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new MedicloudException(
                        "El miembro del staff no fue encontrado en la clínica activa.",
                        ErrorCodes.STAFF_NOT_FOUND, 404));

        StaffResponse.DoctorCredentialsDetailDto credentialsDetail = null;
        if (StaffRole.DOCTOR.equals(cs.getRole())) {
            DoctorProfile dp = doctorProfileRepository.findByClinicStaffId(cs.getId()).orElse(null);
            if (dp != null) {
                credentialsDetail = StaffResponse.DoctorCredentialsDetailDto.builder()
                        .cedulaProfesional(dp.getCedulaProfesional())
                        .cedulaEspecialidad(dp.getCedulaEspecialidad())
                        .especialidad(dp.getEspecialidad())
                        .subEspecialidad(dp.getSubEspecialidad())
                        .universidadEgreso(dp.getUniversidadEgreso())
                        .anioEgreso(dp.getAnioEgreso())
                        .institucionEspecialidad(dp.getInstitucionEspecialidad())
                        .credentialStatus(dp.getCredentialStatus().getValue())
                        .verifiedAt(dp.getVerifiedAt())
                        .verifiedByUserId(dp.getVerifiedByUserId())
                        .build();
            }
        }

        User user = cs.getUser();
        return StaffResponse.builder()
                .id(cs.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(cs.getRole().getValue())
                .roleLabel(getRoleLabel(cs.getRole()))
                .isActive(cs.getClinic() == null || cs.getClinic().isActive())
                .doctorCredentials(credentialsDetail)
                .build();
    }

    /**
     * Verifica y marca como válidas las credenciales profesionales de un médico.
     * <p>
     * Cambia el estado de la credencial a {@code activo} si hay cédula profesional,
     * o mantiene {@code en_tramite} si solo existe documento comprobatorio.
     * Solo puede ser ejecutado por un administrador de la clínica.
     * </p>
     *
     * @param clinicId         ID de la clínica activa.
     * @param staffId          ID del miembro del staff (debe ser doctor).
     * @param verifiedByUserId ID del administrador que realiza la verificación.
     * @return {@link VerifyCredentialsResponse} con el estado actualizado y la fecha de verificación.
     * @throws MedicloudException Si el miembro no existe, o no tiene rol de doctor.
     */
    @Transactional
    public VerifyCredentialsResponse verifyCredentials(UUID clinicId, UUID staffId, UUID verifiedByUserId) {
        ClinicStaff verifier = clinicStaffRepository.findByUserIdAndClinicId(verifiedByUserId, clinicId)
                .orElseThrow(() -> new MedicloudException(
                        "El usuario que verifica no pertenece a la clínica activa.",
                        ErrorCodes.CLINIC_ACCESS_DENIED, 403));

        if (!StaffRole.ADMIN.equals(verifier.getRole()) && !StaffRole.CLINIC_ADMIN.equals(verifier.getRole())) {
            throw new MedicloudException(
                    "Solo un administrador de clínica puede verificar credenciales.",
                    ErrorCodes.FORBIDDEN, 403);
        }

        ClinicStaff cs = clinicStaffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new MedicloudException(
                        "El miembro del staff no fue encontrado en la clínica activa.",
                        ErrorCodes.STAFF_NOT_FOUND, 404));

        if (!StaffRole.DOCTOR.equals(cs.getRole())) {
            throw new MedicloudException(
                    "El miembro del staff no tiene el rol de médico.",
                    ErrorCodes.STAFF_NOT_A_DOCTOR, 422);
        }

        DoctorProfile dp = doctorProfileRepository.findByClinicStaffId(cs.getId())
                .orElseThrow(() -> new MedicloudException(
                        "No se encontraron credenciales para este doctor.",
                        ErrorCodes.STAFF_NOT_FOUND, 404));

        Instant now = Instant.now();
        DoctorCredentialStatus newStatus = dp.getCedulaProfesional() != null
                ? DoctorCredentialStatus.ACTIVO
                : DoctorCredentialStatus.EN_TRAMITE;

        dp.setCredentialStatus(newStatus);
        dp.setVerifiedAt(now);
        dp.setVerifiedByUserId(verifiedByUserId);
        doctorProfileRepository.save(dp);

        log.info("Credentials verified for staffId: {} by userId: {}. Status: {}", staffId, verifiedByUserId, newStatus);

        return VerifyCredentialsResponse.builder()
                .staffId(staffId)
                .credentialStatus(newStatus.getValue())
                .verifiedAt(now)
                .build();
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Deserializa las credenciales del doctor desde la invitación y persiste un {@link DoctorProfile}.
     * Se llama únicamente desde {@code acceptInvitation()} tras crear el {@link ClinicStaff}.
     */
    private void createDoctorProfile(ClinicStaff clinicStaff, StaffInvitation invitation) {
        DoctorCredentialsDto creds;
        try {
            creds = objectMapper.treeToValue(invitation.getDoctorCredentialsJson(), DoctorCredentialsDto.class);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo deserializar doctor_credentials_json para invitationId={}", invitation.getId());
            return;
        }

        DoctorCredentialStatus initialStatus = creds.getCedulaProfesional() != null
                ? DoctorCredentialStatus.ACTIVO
                : DoctorCredentialStatus.EN_TRAMITE;

        DoctorProfile profile = DoctorProfile.builder()
                .clinicStaffId(clinicStaff.getId())
                .clinicId(clinicStaff.getClinicId())
                .cedulaProfesional(creds.getCedulaProfesional())
                .cedulaEspecialidad(creds.getCedulaEspecialidad())
                .especialidad(creds.getEspecialidad())
                .subEspecialidad(creds.getSubEspecialidad())
                .universidadEgreso(creds.getUniversidadEgreso())
                .anioEgreso(creds.getAnioEgreso())
                .institucionEspecialidad(creds.getInstitucionEspecialidad())
                .documentoTramiteUrl(creds.getDocumentoTramiteUrl())
                .credentialStatus(initialStatus)
                .build();

        doctorProfileRepository.save(profile);
        log.info("DoctorProfile creado para clinicStaffId={}, status={}", clinicStaff.getId(), initialStatus);
    }

    /**
     * Genera la respuesta de aceptación de invitación con tokens JWT y refresh token.
     *
     * @param user       Usuario autenticado tras aceptar la invitación.
     * @param invitation Invitación aceptada (para obtener datos de la clínica).
     * @return {@link AcceptInvitationResponse} completo.
     */
    private AcceptInvitationResponse generateAcceptInvitationResponse(User user, StaffInvitation invitation) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        List<ClinicStaff> staffList = clinicStaffRepository.findByUserId(user.getId());
        List<String> roles = staffList.stream().map(cs -> cs.getRole().getValue()).distinct().toList();
        List<String> clinics = staffList.stream().map(cs -> cs.getClinicId().toString()).distinct().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://api.medicloud.mx")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("clinics", clinics)
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        String rawRefreshToken = tokenHelper.generateRandomToken();
        String tokenHash = tokenHelper.hashToken(rawRefreshToken);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceLabel("Web App")
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        UserResponse userProfile = profileService.getProfile(user);
        List<UserClinicDto> clinicDtos = userProfile.getClinics();

        AcceptInvitationResponse.ClinicSummaryDto joinedClinic = AcceptInvitationResponse.ClinicSummaryDto.builder()
                .id(invitation.getClinicId())
                .name(invitation.getClinic() != null ? invitation.getClinic().getName() : "Clínica")
                .build();

        AcceptInvitationResponse.UserSummaryDto userSummary = AcceptInvitationResponse.UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .clinics(clinicDtos)
                .build();

        return AcceptInvitationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken(rawRefreshToken)
                .user(userSummary)
                .joinedClinic(joinedClinic)
                .build();
    }

    /**
     * Valida las credenciales del doctor y construye el DTO de respuesta con el estado calculado.
     *
     * @param creds DTO con los datos de credenciales del médico.
     * @return DTO de credenciales con el {@code credential_status} calculado.
     */
    private InviteStaffResponse.DoctorCredentialsResponseDto validateAndBuildDoctorCredentials(
            DoctorCredentialsDto creds) {
        if (creds == null) {
            throw new MedicloudException(
                    "Las credenciales del doctor son requeridas cuando el rol es 'doctor'.",
                    ErrorCodes.DOCTOR_CREDENTIAL_REQUIRED, 422);
        }
        if (creds.getUniversidadEgreso() == null || creds.getUniversidadEgreso().isBlank()) {
            throw new MedicloudException(
                    "El campo 'universidad_egreso' es requerido para doctores.",
                    ErrorCodes.DOCTOR_CREDENTIAL_REQUIRED, 422);
        }
        if (creds.getAnioEgreso() == null) {
            throw new MedicloudException(
                    "El campo 'anio_egreso' es requerido para doctores.",
                    ErrorCodes.DOCTOR_CREDENTIAL_REQUIRED, 422);
        }
        if (creds.getCedulaProfesional() == null && creds.getDocumentoTramiteUrl() == null) {
            throw new MedicloudException(
                    "Se requiere 'cedula_profesional' o 'documento_tramite_url' para el registro del doctor.",
                    ErrorCodes.DOCTOR_CREDENTIAL_REQUIRED, 422);
        }

        String credentialStatus = creds.getCedulaProfesional() != null ? "activo" : "en_tramite";

        return InviteStaffResponse.DoctorCredentialsResponseDto.builder()
                .universidadEgreso(creds.getUniversidadEgreso())
                .anioEgreso(creds.getAnioEgreso())
                .especialidad(creds.getEspecialidad())
                .subEspecialidad(creds.getSubEspecialidad())
                .cedulaProfesional(creds.getCedulaProfesional())
                .cedulaEspecialidad(creds.getCedulaEspecialidad())
                .institucionEspecialidad(creds.getInstitucionEspecialidad())
                .documentoTramiteUrl(creds.getDocumentoTramiteUrl())
                .credentialStatus(credentialStatus)
                .build();
    }

    /**
     * Convierte una cadena de texto al enum {@link StaffRole} correspondiente.
     *
     * @param role Subcadena del rol.
     * @return El enum correspondiente.
     */
    private StaffRole parseRole(String role) {
        try {
            return StaffRole.fromValue(role);
        } catch (Exception e) {
            throw new MedicloudException("Rol no reconocido: " + role, ErrorCodes.VALIDATION_ERROR, 422);
        }
    }

    /**
     * Obtiene la etiqueta legible de un rol.
     *
     * @param role Rol del staff.
     * @return Etiqueta del rol.
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
