package com.hrms.reference.repository;

import com.hrms.reference.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

    List<Calendar> findByCompanyId(UUID companyId);

    Optional<Calendar> findByCompanyIdAndYear(UUID companyId, int year);
}
