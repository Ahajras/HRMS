package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    List<PayrollResult> findByRunIdOrderByEmployeeId(UUID runId);

    void deleteByRunId(UUID runId);
}
