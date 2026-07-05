package com.hrms.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LeaveRequestDto {
    private UUID id;
    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private UUID leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate returnDate;
    private BigDecimal totalDays = BigDecimal.ZERO;
    private String reason;
    private String status = "DRAFT";
    private boolean requiresTicket;
    private String ticketFrom;
    private String ticketTo;
    private LocalDate travelDate;
    private LocalDate returnTravelDate;
    private String destination;
    private String travelRemarks;
    private String contactPhone;
    private String contactEmail;
    private String addressDuringLeave;
    private String emergencyContactName;
    private String emergencyContactPhone;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(UUID leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeCode() { return leaveTypeCode; }
    public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
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
}
