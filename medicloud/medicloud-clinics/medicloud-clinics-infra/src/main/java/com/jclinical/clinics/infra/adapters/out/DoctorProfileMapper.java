package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.DoctorProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DoctorProfileMapper {

    DoctorProfileEntity toEntity(DoctorProfile domain);

    DoctorProfile toDomain(DoctorProfileEntity entity);
}
