package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.ShiftDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShiftDayRepository extends JpaRepository<ShiftDay, UUID> {

    List<ShiftDay> findByShiftId(UUID shiftId);

    void deleteByShiftId(UUID shiftId);
}
