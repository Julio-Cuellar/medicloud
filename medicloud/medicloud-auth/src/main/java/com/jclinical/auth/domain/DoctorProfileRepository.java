package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    Optional<DoctorProfile> findByClinicStaffId(UUID clinicStaffId);
}
