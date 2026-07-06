package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    List<PayrollResult> findByRunIdOrderByEmployeeId(UUID runId);

    void deleteByRunId(UUID runId);

    @Query("""
            select count(r), coalesce(sum(r.gross), 0), coalesce(sum(r.net), 0)
            from PayrollResult r
            where r.runId = :runId
            """)
    Object[] summarizeRun(@Param("runId") UUID runId);
}
