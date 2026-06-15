package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum StaffRole {
    ADMIN,
    DOCTOR,
    RECEPTIONIST,
    ASSISTANT,
    ACCOUNTANT,
    CLEANING,
    CLINIC_ADMIN;

    public String getValue() {
        return name().toLowerCase();
    }

    public static StaffRole fromValue(String value) {
        if (value == null) return null;
        return StaffRole.valueOf(value.toUpperCase());
    }

    @Converter(autoApply = true)
    public static class StaffRoleConverter implements AttributeConverter<StaffRole, String> {
        @Override
        public String convertToDatabaseColumn(StaffRole attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        @Override
        public StaffRole convertToEntityAttribute(String dbData) {
            return dbData == null ? null : StaffRole.fromValue(dbData);
        }
    }
}
