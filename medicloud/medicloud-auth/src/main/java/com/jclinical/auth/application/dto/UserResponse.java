package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    
    @JsonProperty("full_name")
    private String fullName;
    
    private String email;
    private String phone;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("email_verified")
    private boolean emailVerified;
    
    @JsonProperty("theme_preference")
    private String themePreference;
    
    @JsonProperty("is_active")
    private boolean isActive;
    
    @JsonProperty("last_login_at")
    private Instant lastLoginAt;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    private List<UserClinicDto> clinics;
}
