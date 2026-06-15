package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Rol asignado al miembro del personal dentro de una clínica o en la plataforma general.
 */
public enum StaffRole {
    /** Administrador general de la plataforma con todos los privilegios. */
    ADMIN,
    /** Médico/Doctor que provee consultas clínicas y tratamientos a pacientes. */
    DOCTOR,
    /** Recepcionista encargado de agendas, citas y atención en mostrador. */
    RECEPTIONIST,
    /** Asistente del personal médico o administrativo. */
    ASSISTANT,
    /** Contador encargado de la facturación y finanzas. */
    ACCOUNTANT,
    /** Personal encargado de servicios de limpieza y mantenimiento. */
    CLEANING,
    /** Administrador específico de una clínica con facultades de configuración locales. */
    CLINIC_ADMIN;

    /**
     * Obtiene el valor textual en minúsculas del rol para persistencia o API.
     *
     * @return El nombre del enum en minúsculas.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    /**
     * Mapea un valor textual de rol a su correspondiente constante enum.
     *
     * @param value El valor textual a mapear.
     * @return La constante {@link StaffRole} equivalente, o {@code null} si el valor provisto es nulo.
     */
    public static StaffRole fromValue(String value) {
        if (value == null) return null;
        return StaffRole.valueOf(value.toUpperCase());
    }

    /**
     * Conversor JPA automático para mapear el enum {@link StaffRole} a su representación de base de datos.
     */
    @Converter(autoApply = true)
    public static class StaffRoleConverter implements AttributeConverter<StaffRole, String> {
        
        /**
         * Convierte el enum del rol a la cadena de base de datos correspondiente.
         */
        @Override
        public String convertToDatabaseColumn(StaffRole attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        /**
         * Mapea la cadena de base de datos al correspondiente enum.
         */
        @Override
        public StaffRole convertToEntityAttribute(String dbData) {
            return dbData == null ? null : StaffRole.fromValue(dbData);
        }
    }
}
