package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.PayrollCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollCalendarRepository extends JpaRepository<PayrollCalendar, UUID> {

    List<PayrollCalendar> findByCompanyIdOrderByCode(UUID companyId);

    Optional<PayrollCalendar> findByCompanyIdAndCode(UUID companyId, String code);
}
