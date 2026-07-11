package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResultLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PayrollResultLineRepository extends JpaRepository<PayrollResultLine, UUID> {

    List<PayrollResultLine> findByResultIdOrderBySortOrderAsc(UUID resultId);

    /** Batch variant — one query for a whole page of results, not one per result. */
    List<PayrollResultLine> findByResultIdInOrderBySortOrderAsc(Collection<UUID> resultIds);

    /** Audit Tools — cascade-delete every line for a run's results, before
     * the results and run themselves get deleted. */
    @Modifying
    @Query("delete from PayrollResultLine l where l.resultId in (select r.id from PayrollResult r where r.runId = :runId)")
    void deleteByRunId(@Param("runId") UUID runId);
}
