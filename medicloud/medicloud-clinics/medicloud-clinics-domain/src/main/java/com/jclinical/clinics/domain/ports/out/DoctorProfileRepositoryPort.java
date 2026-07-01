package com.jclinical.clinics.domain.ports.out;

import com.jclinical.clinics.domain.model.DoctorProfile;

import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepositoryPort {
    DoctorProfile save(DoctorProfile profile);
    Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId);
}
