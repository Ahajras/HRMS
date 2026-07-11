package com.hrms.benefits.repository;

import com.hrms.benefits.domain.TicketFare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketFareRepository extends JpaRepository<TicketFare, UUID> {
    List<TicketFare> findByCompanyIdOrderByFromAirportCodeAscToAirportCodeAsc(UUID companyId);

    @Query("""
            select f from TicketFare f
            where f.companyId = :companyId
              and upper(f.fromAirportCode) = upper(:fromAirportCode)
              and upper(f.toAirportCode) = upper(:toAirportCode)
              and upper(coalesce(f.status, '')) = 'ACTIVE'
              and f.effectiveFrom <= :asOf
              and (f.effectiveTo is null or f.effectiveTo >= :asOf)
            order by f.effectiveFrom desc, f.createdAt desc
            """)
    List<TicketFare> findActiveRoute(@Param("companyId") UUID companyId,
                                     @Param("fromAirportCode") String fromAirportCode,
                                     @Param("toAirportCode") String toAirportCode,
                                     @Param("asOf") LocalDate asOf);

    default Optional<TicketFare> findBest(UUID companyId, String fromAirportCode, String toAirportCode, LocalDate asOf) {
        return findActiveRoute(companyId, fromAirportCode, toAirportCode, asOf).stream().findFirst();
    }
}
