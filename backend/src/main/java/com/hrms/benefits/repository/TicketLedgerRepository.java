package com.hrms.benefits.repository;

import com.hrms.benefits.domain.TicketLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketLedgerRepository extends JpaRepository<TicketLedger, UUID> {
    List<TicketLedger> findByCompanyIdAndEmployeeIdOrderByEntryDateDesc(UUID companyId, UUID employeeId);
    List<TicketLedger> findByCompanyIdAndEmployeeIdAndStatusAndEntryDateLessThanEqual(UUID companyId, UUID employeeId, String status, LocalDate asOf);
    Optional<TicketLedger> findByCompanyIdAndLeaveRequestIdAndStatus(UUID companyId, UUID leaveRequestId, String status);
}
