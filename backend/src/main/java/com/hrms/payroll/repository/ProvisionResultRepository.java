package com.hrms.payroll.repository;

import com.hrms.payroll.domain.ProvisionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvisionResultRepository extends JpaRepository<ProvisionResult, UUID> {

    List<ProvisionResult> findByRunIdOrderByEmployeeNumberAsc(UUID runId);

    void deleteByRunId(UUID runId);
}
