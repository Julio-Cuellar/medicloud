package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum DoctorCredentialStatus {
    ACTIVO,
    EN_TRAMITE,
    SUSPENDIDO;

    public String getValue() {
        return name().toLowerCase();
    }

    public static DoctorCredentialStatus fromValue(String value) {
        if (value == null) return null;
        return DoctorCredentialStatus.valueOf(value.toUpperCase().replace(" ", "_"));
    }

    @Converter(autoApply = true)
    public static class DoctorCredentialStatusConverter implements AttributeConverter<DoctorCredentialStatus, String> {
        @Override
        public String convertToDatabaseColumn(DoctorCredentialStatus attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        @Override
        public DoctorCredentialStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : DoctorCredentialStatus.fromValue(dbData);
        }
    }
}
