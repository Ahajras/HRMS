package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.PayrollWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollWeekRepository extends JpaRepository<PayrollWeek, UUID> {

    List<PayrollWeek> findByPeriodIdOrderByWeekNo(UUID periodId);

    void deleteByPeriodId(UUID periodId);
}
