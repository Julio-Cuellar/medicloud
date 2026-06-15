package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Estado de validación/vigencia de las credenciales profesionales de un médico (Cédula Profesional).
 */
public enum DoctorCredentialStatus {
    /** Credencial profesional activa y válida. */
    ACTIVO,
    /** Credencial profesional en proceso de validación o trámite ante las autoridades. */
    EN_TRAMITE,
    /** Credencial profesional suspendida o inhabilitada temporal o permanentemente. */
    SUSPENDIDO;

    /**
     * Obtiene el valor textual en minúsculas del estado para persistencia o API.
     *
     * @return El nombre del enum en minúsculas.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    /**
     * Mapea un valor textual de estado a su correspondiente constante enum.
     *
     * @param value El valor textual a mapear.
     * @return La constante {@link DoctorCredentialStatus} equivalente, o {@code null} si el valor provisto es nulo.
     */
    public static DoctorCredentialStatus fromValue(String value) {
        if (value == null) return null;
        return DoctorCredentialStatus.valueOf(value.toUpperCase().replace(" ", "_"));
    }

    /**
     * Conversor JPA automático para mapear el enum {@link DoctorCredentialStatus} a su representación de base de datos.
     */
    @Converter(autoApply = true)
    public static class DoctorCredentialStatusConverter implements AttributeConverter<DoctorCredentialStatus, String> {
        
        /**
         * Convierte el enum del estado de credencial a la cadena de base de datos correspondiente.
         */
        @Override
        public String convertToDatabaseColumn(DoctorCredentialStatus attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        /**
         * Mapea la cadena de base de datos al correspondiente enum.
         */
        @Override
        public DoctorCredentialStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : DoctorCredentialStatus.fromValue(dbData);
        }
    }
}
