package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    List<PayrollResult> findByRunIdOrderByEmployeeId(UUID runId);

    /** Paginated — used by the run detail screen so opening a large run
     * (thousands of employees) stays fast. */
    Page<PayrollResult> findByRunId(UUID runId, Pageable pageable);

    /** Paginated + restricted to a specific set of employees (used for search). */
    Page<PayrollResult> findByRunIdAndEmployeeIdIn(UUID runId, Collection<UUID> employeeIds, Pageable pageable);

    void deleteByRunId(UUID runId);

    @Query("""
            select count(r), coalesce(sum(r.gross), 0), coalesce(sum(r.net), 0)
            from PayrollResult r
            where r.runId = :runId
            """)
    List<Object[]> summarizeRun(@Param("runId") UUID runId);
}
