package com.jclinical.shared.domain;

import lombok.Getter;
import java.util.Map;
import java.util.Collections;

@Getter
public class ValidationException extends MedicloudException {
    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message, ErrorCodes.VALIDATION_ERROR, 422);
        this.errors = Collections.emptyMap();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message, ErrorCodes.VALIDATION_ERROR, 422);
        this.errors = errors != null ? errors : Collections.emptyMap();
    }
}
