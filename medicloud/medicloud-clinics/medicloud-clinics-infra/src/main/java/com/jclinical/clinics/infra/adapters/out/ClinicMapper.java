package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.Clinic;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClinicMapper {

    ClinicEntity toEntity(Clinic domain);

    Clinic toDomain(ClinicEntity entity);

    com.jclinical.clinics.infra.adapters.in.web.dto.ClinicResponse toResponse(Clinic domain);
}
