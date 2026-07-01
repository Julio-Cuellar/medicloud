package com.jclinical.patients.infra.adapters.out;

import com.jclinical.patients.domain.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "addressStreet", source = "address.street")
    @Mapping(target = "addressOutdoorNumber", source = "address.outdoorNumber")
    @Mapping(target = "addressIndoorNumber", source = "address.indoorNumber")
    @Mapping(target = "addressColonia", source = "address.colonia")
    @Mapping(target = "addressMunicipality", source = "address.municipality")
    @Mapping(target = "addressState", source = "address.state")
    @Mapping(target = "addressZipCode", source = "address.zipCode")
    @Mapping(target = "emergencyContactFullName", source = "emergencyContact.fullName")
    @Mapping(target = "emergencyContactRelationship", source = "emergencyContact.relationship")
    @Mapping(target = "emergencyContactPhone", source = "emergencyContact.phone")
    PatientEntity toEntity(Patient domain);

    @Mapping(target = "address.street", source = "addressStreet")
    @Mapping(target = "address.outdoorNumber", source = "addressOutdoorNumber")
    @Mapping(target = "address.indoorNumber", source = "addressIndoorNumber")
    @Mapping(target = "address.colonia", source = "addressColonia")
    @Mapping(target = "address.municipality", source = "addressMunicipality")
    @Mapping(target = "address.state", source = "addressState")
    @Mapping(target = "address.zipCode", source = "addressZipCode")
    @Mapping(target = "emergencyContact.fullName", source = "emergencyContactFullName")
    @Mapping(target = "emergencyContact.relationship", source = "emergencyContactRelationship")
    @Mapping(target = "emergencyContact.phone", source = "emergencyContactPhone")
    Patient toDomain(PatientEntity entity);
}
