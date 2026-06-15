package com.jclinical.shared.domain;

/**
 * Códigos de error estándar utilizados en toda la plataforma Medicloud.
 * Permite identificar de manera unívoca la causa del error en el cliente.
 */
public final class ErrorCodes {
    
    private ErrorCodes() {}

    /** Error de validación de campos de entrada. */
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    /** Credenciales de acceso incorrectas. */
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    /** El correo electrónico del usuario no ha sido verificado. */
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    /** El usuario está marcado como inactivo. */
    public static final String USER_INACTIVE = "USER_INACTIVE";
    /** La cuenta de usuario está bloqueada temporalmente. */
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    /** La contraseña provista es débil o no cumple con las políticas. */
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    /** La contraseña coincide con una utilizada recientemente. */
    public static final String PASSWORD_RECENTLY_USED = "PASSWORD_RECENTLY_USED";
    /** Se requiere especificar una contraseña para la acción. */
    public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    
    /** El token de refresco (Refresh Token) no es válido. */
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    /** El token de refresco ha expirado. */
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    /** El token de refresco ya ha sido utilizado (detección de reutilización). */
    public static final String REFRESH_TOKEN_REUSED = "REFRESH_TOKEN_REUSED";
    
    /** El token de invitación de personal médico no es válido. */
    public static final String INVITATION_TOKEN_INVALID = "INVITATION_TOKEN_INVALID";
    /** El token de invitación de personal médico ha expirado. */
    public static final String INVITATION_TOKEN_EXPIRED = "INVITATION_TOKEN_EXPIRED";
    
    /** El token de restablecimiento de contraseña no es válido. */
    public static final String RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID";
    /** El token de restablecimiento de contraseña ha expirado. */
    public static final String RESET_TOKEN_EXPIRED = "RESET_TOKEN_EXPIRED";
    
    /** El token de verificación de correo no es válido. */
    public static final String VERIFICATION_TOKEN_INVALID = "VERIFICATION_TOKEN_INVALID";
    /** El token de verificación de correo ha expirado. */
    public static final String VERIFICATION_TOKEN_EXPIRED = "VERIFICATION_TOKEN_EXPIRED";
    
    /** Se requieren las credenciales profesionales del doctor. */
    public static final String DOCTOR_CREDENTIAL_REQUIRED = "DOCTOR_CREDENTIAL_REQUIRED";
    /** Las credenciales profesionales del doctor están suspendidas. */
    public static final String DOCTOR_CREDENTIAL_SUSPENDED = "DOCTOR_CREDENTIAL_SUSPENDED";
    /** No se encontró al miembro del personal de la clínica. */
    public static final String STAFF_NOT_FOUND = "STAFF_NOT_FOUND";
    /** El miembro del personal no tiene asignado el rol de doctor. */
    public static final String STAFF_NOT_A_DOCTOR = "STAFF_NOT_A_DOCTOR";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    
    /** El usuario no tiene permisos para acceder a esta clínica. */
    public static final String CLINIC_ACCESS_DENIED = "CLINIC_ACCESS_DENIED";
    /** Acción prohibida para el usuario actual. */
    public static final String FORBIDDEN = "FORBIDDEN";
    /** El recurso solicitado no existe. */
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    /** Se superó el límite de peticiones permitido. */
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    /** Error interno genérico del servidor. */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
