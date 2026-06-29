package com.hrms.crew.repository;

import com.hrms.crew.domain.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrewRepository extends JpaRepository<Crew, UUID> {

    List<Crew> findByCompanyIdOrderByCode(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    boolean existsByCompanyIdAndProjectIdAndCode(UUID companyId, UUID projectId, String code);
}
