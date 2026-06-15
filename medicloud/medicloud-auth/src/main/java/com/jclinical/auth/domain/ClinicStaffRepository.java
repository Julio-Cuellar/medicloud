package com.jclinical.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, UUID> {
    List<ClinicStaff> findByUserId(UUID userId);
}
