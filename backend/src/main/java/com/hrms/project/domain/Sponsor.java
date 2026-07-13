package com.hrms.project.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A Qatar WPS "Employer Establishment" — the legal entity registered with
 * the Ministry of Labour / Qatar Central Bank that a SIF file is generated
 * for. Multiple projects can share one sponsor; a SIF file always covers
 * exactly one sponsor (mixing establishments in one file is not allowed
 * by the WPS specification).
 */
@Entity
@Table(name = "sponsor")
public class Sponsor extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Employer Establishment ID (EID) — 7 or 8 digits per QCB spec. */
    @Column(name = "establishment_eid", nullable = false, length = 20)
    private String establishmentEid;

    /** Only used if the Payer is an individual rather than the
     * establishment itself — otherwise leave blank. */
    @Column(name = "payer_qid", length = 20)
    private String payerQid;

    @Column(name = "payer_bank_code", nullable = false, length = 10)
    private String payerBankCode;

    @Column(name = "payer_iban", nullable = false, length = 34)
    private String payerIban;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEstablishmentEid() { return establishmentEid; }
    public void setEstablishmentEid(String establishmentEid) { this.establishmentEid = establishmentEid; }
    public String getPayerQid() { return payerQid; }
    public void setPayerQid(String payerQid) { this.payerQid = payerQid; }
    public String getPayerBankCode() { return payerBankCode; }
    public void setPayerBankCode(String payerBankCode) { this.payerBankCode = payerBankCode; }
    public String getPayerIban() { return payerIban; }
    public void setPayerIban(String payerIban) { this.payerIban = payerIban; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
