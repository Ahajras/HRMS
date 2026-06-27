package com.hrms.migration.repository;

import com.hrms.migration.domain.LegacyEmployeeRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Access to the faithful legacy-snapshot archive ({@link LegacyEmployeeRaw}).
 * Natural key for idempotent upsert is (company_id, employee_number).
 */
public interface LegacyEmployeeRawRepository extends JpaRepository<LegacyEmployeeRaw, UUID> {

    Optional<LegacyEmployeeRaw> findByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);

    Optional<LegacyEmployeeRaw> findByEmployeeId(UUID employeeId);
}
