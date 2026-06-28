package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {

    List<PayrollPeriod> findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(UUID companyId);

    List<PayrollPeriod> findByCompanyIdAndPeriodYearOrderByPeriodMonth(UUID companyId, int periodYear);

    Optional<PayrollPeriod> findByCompanyIdAndCalendarIdAndPeriodYearAndPeriodMonth(
            UUID companyId, UUID calendarId, int periodYear, int periodMonth);

    boolean existsByCompanyIdAndCalendarIdAndPeriodYear(UUID companyId, UUID calendarId, int periodYear);
}
