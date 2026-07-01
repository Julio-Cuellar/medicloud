package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.ClinicStaff;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClinicStaffMapper {

    ClinicStaffEntity toEntity(ClinicStaff domain);

    ClinicStaff toDomain(ClinicStaffEntity entity);
}
