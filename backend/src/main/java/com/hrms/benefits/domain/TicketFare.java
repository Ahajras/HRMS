package com.hrms.benefits.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_fare")
public class TicketFare extends AuditableEntity implements EffectiveDated {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "from_airport_code", nullable = false, length = 20)
    private String fromAirportCode;
    @Column(name = "to_airport_code", nullable = false, length = 20)
    private String toAirportCode;
    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";
    @Column(name = "provider", length = 50)
    private String provider;
    @Column(name = "provider_offer_id", length = 120)
    private String providerOfferId;
    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;
    @Column(name = "remarks", length = 500)
    private String remarks;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getFromAirportCode() { return fromAirportCode; }
    public void setFromAirportCode(String fromAirportCode) { this.fromAirportCode = fromAirportCode; }
    public String getToAirportCode() { return toAirportCode; }
    public void setToAirportCode(String toAirportCode) { this.toAirportCode = toAirportCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    @Override
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    @Override
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderOfferId() { return providerOfferId; }
    public void setProviderOfferId(String providerOfferId) { this.providerOfferId = providerOfferId; }
    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
