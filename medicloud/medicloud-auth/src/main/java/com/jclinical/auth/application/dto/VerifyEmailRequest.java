package com.jclinical.auth.application.dto;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String token;
}
