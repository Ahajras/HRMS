package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, UUID> {

    List<PayrollComponent> findByCompanyIdOrderByPriority(UUID companyId);

    List<PayrollComponent> findByCompanyIdAndCategoryOrderByPriority(UUID companyId, String category);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
