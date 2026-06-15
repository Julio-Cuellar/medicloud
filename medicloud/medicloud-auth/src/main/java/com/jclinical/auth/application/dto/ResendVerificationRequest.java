package com.jclinical.auth.application.dto;

import lombok.Data;

@Data
public class ResendVerificationRequest {
    private String email;
}
