package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {

    Optional<Timesheet> findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
            UUID companyId, UUID employeeId, int periodYear, int periodMonth);

    List<Timesheet> findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
            UUID companyId, int periodYear, int periodMonth);

    List<Timesheet> findByPeriodId(UUID periodId);
}
