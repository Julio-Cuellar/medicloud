package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MedicalHistoryMapper {
    MedicalHistoryEntity toEntity(MedicalHistory domain);
    MedicalHistory toDomain(MedicalHistoryEntity entity);
}
