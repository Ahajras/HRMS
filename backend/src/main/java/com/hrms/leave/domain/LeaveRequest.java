package com.hrms.leave.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_request")
public class LeaveRequest extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "return_date")
    private LocalDate returnDate;
    @Column(name = "total_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalDays = BigDecimal.ZERO;
    @Column(name = "reason", length = 500)
    private String reason;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "requires_ticket", nullable = false)
    private boolean requiresTicket;
    @Column(name = "ticket_from", length = 100)
    private String ticketFrom;
    @Column(name = "ticket_to", length = 100)
    private String ticketTo;
    @Column(name = "travel_date")
    private LocalDate travelDate;
    @Column(name = "return_travel_date")
    private LocalDate returnTravelDate;
    @Column(name = "destination", length = 200)
    private String destination;
    @Column(name = "travel_remarks", length = 500)
    private String travelRemarks;
    @Column(name = "contact_phone", length = 80)
    private String contactPhone;
    @Column(name = "contact_email", length = 150)
    private String contactEmail;
    @Column(name = "address_during_leave", length = 500)
    private String addressDuringLeave;
    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 80)
    private String emergencyContactPhone;
    @Column(name = "supervisor_approved_at")
    private Instant supervisorApprovedAt;
    @Column(name = "supervisor_approved_by", length = 100)
    private String supervisorApprovedBy;
    @Column(name = "hr_approved_at")
    private Instant hrApprovedAt;
    @Column(name = "hr_approved_by", length = 100)
    private String hrApprovedBy;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(UUID leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isRequiresTicket() { return requiresTicket; }
    public void setRequiresTicket(boolean requiresTicket) { this.requiresTicket = requiresTicket; }
    public String getTicketFrom() { return ticketFrom; }
    public void setTicketFrom(String ticketFrom) { this.ticketFrom = ticketFrom; }
    public String getTicketTo() { return ticketTo; }
    public void setTicketTo(String ticketTo) { this.ticketTo = ticketTo; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public LocalDate getReturnTravelDate() { return returnTravelDate; }
    public void setReturnTravelDate(LocalDate returnTravelDate) { this.returnTravelDate = returnTravelDate; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getTravelRemarks() { return travelRemarks; }
    public void setTravelRemarks(String travelRemarks) { this.travelRemarks = travelRemarks; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAddressDuringLeave() { return addressDuringLeave; }
    public void setAddressDuringLeave(String addressDuringLeave) { this.addressDuringLeave = addressDuringLeave; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public Instant getSupervisorApprovedAt() { return supervisorApprovedAt; }
    public void setSupervisorApprovedAt(Instant supervisorApprovedAt) { this.supervisorApprovedAt = supervisorApprovedAt; }
    public String getSupervisorApprovedBy() { return supervisorApprovedBy; }
    public void setSupervisorApprovedBy(String supervisorApprovedBy) { this.supervisorApprovedBy = supervisorApprovedBy; }
    public Instant getHrApprovedAt() { return hrApprovedAt; }
    public void setHrApprovedAt(Instant hrApprovedAt) { this.hrApprovedAt = hrApprovedAt; }
    public String getHrApprovedBy() { return hrApprovedBy; }
    public void setHrApprovedBy(String hrApprovedBy) { this.hrApprovedBy = hrApprovedBy; }
}
