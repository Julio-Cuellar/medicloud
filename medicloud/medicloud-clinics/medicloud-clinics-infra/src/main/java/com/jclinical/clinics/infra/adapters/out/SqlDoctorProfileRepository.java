package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.DoctorProfile;
import com.jclinical.clinics.domain.ports.out.DoctorProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlDoctorProfileRepository implements DoctorProfileRepositoryPort {

    private final SpringDataDoctorProfileRepository springDataRepository;
    private final DoctorProfileMapper mapper;

    @Override
    public DoctorProfile save(DoctorProfile profile) {
        DoctorProfileEntity entity = mapper.toEntity(profile);
        DoctorProfileEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId) {
        return springDataRepository.findByClinicStaffId(clinicStaffId)
                .map(mapper::toDomain);
    }
}
