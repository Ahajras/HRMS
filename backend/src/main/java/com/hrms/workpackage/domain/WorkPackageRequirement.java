package com.hrms.workpackage.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "work_package_requirement")
public class WorkPackageRequirement extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "work_package_id", nullable = false)
    private UUID workPackageId;
    @Column(name = "job_title_code", nullable = false, length = 40)
    private String jobTitleCode;
    @Column(name = "job_title_name", length = 150)
    private String jobTitleName;
    @Column(name = "required_count", nullable = false)
    private int requiredCount = 1;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWorkPackageId() { return workPackageId; }
    public void setWorkPackageId(UUID workPackageId) { this.workPackageId = workPackageId; }
    public String getJobTitleCode() { return jobTitleCode; }
    public void setJobTitleCode(String jobTitleCode) { this.jobTitleCode = jobTitleCode; }
    public String getJobTitleName() { return jobTitleName; }
    public void setJobTitleName(String jobTitleName) { this.jobTitleName = jobTitleName; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
}
