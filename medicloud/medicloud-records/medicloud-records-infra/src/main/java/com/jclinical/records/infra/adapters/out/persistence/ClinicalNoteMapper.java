package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.VitalSigns;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ClinicalNoteMapper {

    @Mapping(target = "vitalTemp", source = "vitalSigns.temperature")
    @Mapping(target = "vitalBp", source = "vitalSigns.bloodPressure")
    @Mapping(target = "vitalHr", source = "vitalSigns.heartRate")
    @Mapping(target = "vitalRr", source = "vitalSigns.respiratoryRate")
    @Mapping(target = "vitalWeight", source = "vitalSigns.weight")
    @Mapping(target = "vitalHeight", source = "vitalSigns.height")
    @Mapping(target = "vitalBmi", source = "vitalSigns.bmi")
    @Mapping(target = "vitalO2", source = "vitalSigns.oxygenSaturation")
    ClinicalNoteEntity toEntity(ClinicalNote domain);

    @Mapping(target = "vitalSigns", source = "entity", qualifiedByName = "mapVitalSigns")
    ClinicalNote toDomain(ClinicalNoteEntity entity);

    @Named("mapVitalSigns")
    default VitalSigns mapVitalSigns(ClinicalNoteEntity entity) {
        if (entity.getVitalTemp() == null && entity.getVitalBp() == null && entity.getVitalHr() == null
                && entity.getVitalRr() == null && entity.getVitalWeight() == null && entity.getVitalHeight() == null
                && entity.getVitalBmi() == null && entity.getVitalO2() == null) {
            return null;
        }
        return new VitalSigns(
                entity.getVitalTemp(),
                entity.getVitalBp(),
                entity.getVitalHr(),
                entity.getVitalRr(),
                entity.getVitalWeight(),
                entity.getVitalHeight(),
                entity.getVitalBmi(),
                entity.getVitalO2()
        );
    }
}
