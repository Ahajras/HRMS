package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResultLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollResultLineRepository extends JpaRepository<PayrollResultLine, UUID> {

    List<PayrollResultLine> findByResultIdOrderBySortOrderAsc(UUID resultId);
}
