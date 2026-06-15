package com.jclinical.shared.domain;

public final class ErrorCodes {
    
    private ErrorCodes() {}

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String USER_INACTIVE = "USER_INACTIVE";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    public static final String PASSWORD_RECENTLY_USED = "PASSWORD_RECENTLY_USED";
    public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REUSED = "REFRESH_TOKEN_REUSED";
    
    public static final String INVITATION_TOKEN_INVALID = "INVITATION_TOKEN_INVALID";
    public static final String INVITATION_TOKEN_EXPIRED = "INVITATION_TOKEN_EXPIRED";
    
    public static final String RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID";
    public static final String RESET_TOKEN_EXPIRED = "RESET_TOKEN_EXPIRED";
    
    public static final String VERIFICATION_TOKEN_INVALID = "VERIFICATION_TOKEN_INVALID";
    public static final String VERIFICATION_TOKEN_EXPIRED = "VERIFICATION_TOKEN_EXPIRED";
    
    public static final String DOCTOR_CREDENTIAL_REQUIRED = "DOCTOR_CREDENTIAL_REQUIRED";
    public static final String DOCTOR_CREDENTIAL_SUSPENDED = "DOCTOR_CREDENTIAL_SUSPENDED";
    public static final String STAFF_NOT_FOUND = "STAFF_NOT_FOUND";
    public static final String STAFF_NOT_A_DOCTOR = "STAFF_NOT_A_DOCTOR";
    
    public static final String CLINIC_ACCESS_DENIED = "CLINIC_ACCESS_DENIED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
