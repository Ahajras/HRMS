package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeTypeRepository extends JpaRepository<TimeType, UUID> {

    List<TimeType> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);

    Optional<TimeType> findByCompanyIdAndCode(UUID companyId, String code);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
