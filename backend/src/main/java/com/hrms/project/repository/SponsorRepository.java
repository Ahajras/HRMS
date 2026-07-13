package com.hrms.project.repository;

import com.hrms.project.domain.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SponsorRepository extends JpaRepository<Sponsor, UUID> {

    List<Sponsor> findByCompanyIdOrderByCode(UUID companyId);
}
