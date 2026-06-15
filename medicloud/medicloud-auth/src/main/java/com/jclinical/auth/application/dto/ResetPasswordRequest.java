package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    
    @JsonProperty("new_password")
    private String newPassword;
}
