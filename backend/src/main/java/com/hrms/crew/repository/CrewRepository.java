package com.hrms.crew.repository;

import com.hrms.crew.domain.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrewRepository extends JpaRepository<Crew, UUID> {

    List<Crew> findByCompanyIdOrderByCode(UUID companyId);

    List<Crew> findByCompanyIdAndCodeStartingWithOrderByCode(UUID companyId, String prefix);

    List<Crew> findByCompanyIdAndProjectIdAndCodeStartingWithOrderByCode(UUID companyId, UUID projectId, String prefix);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    boolean existsByCompanyIdAndProjectIdAndCode(UUID companyId, UUID projectId, String code);
}
