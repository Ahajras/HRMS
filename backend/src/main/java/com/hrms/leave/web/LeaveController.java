package com.hrms.leave.web;

import com.hrms.leave.dto.LeaveAdjustmentDto;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveRequestDto;
import com.hrms.leave.dto.LeaveTypeDto;
import com.hrms.leave.service.LeaveService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave")
public class LeaveController {
    private final LeaveService service;

    public LeaveController(LeaveService service) {
        this.service = service;
    }

    @GetMapping("/types")
    public List<LeaveTypeDto> types() {
        return service.listTypes();
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveTypeDto createType(@RequestBody LeaveTypeDto dto) {
        return service.saveType(dto);
    }

    @PutMapping("/types/{id}")
    public LeaveTypeDto updateType(@PathVariable UUID id, @RequestBody LeaveTypeDto dto) {
        dto.setId(id);
        return service.saveType(dto);
    }

    @GetMapping("/requests")
    public List<LeaveRequestDto> requests(@RequestParam(required = false) UUID employeeId) {
        return service.listRequests(employeeId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestDto createRequest(@RequestBody LeaveRequestDto dto) {
        return service.saveRequest(dto);
    }

    @PutMapping("/requests/{id}")
    public LeaveRequestDto updateRequest(@PathVariable UUID id, @RequestBody LeaveRequestDto dto) {
        dto.setId(id);
        return service.saveRequest(dto);
    }

    @PostMapping("/requests/{id}/status")
    public LeaveRequestDto setStatus(@PathVariable UUID id, @RequestParam String status) {
        return service.setRequestStatus(id, status);
    }

    @GetMapping("/adjustments")
    public List<LeaveAdjustmentDto> adjustments(@RequestParam UUID employeeId) {
        return service.listAdjustments(employeeId);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveAdjustmentDto createAdjustment(@RequestBody LeaveAdjustmentDto dto) {
        return service.saveAdjustment(dto);
    }

    @PutMapping("/adjustments/{id}")
    public LeaveAdjustmentDto updateAdjustment(@PathVariable UUID id, @RequestBody LeaveAdjustmentDto dto) {
        dto.setId(id);
        return service.saveAdjustment(dto);
    }

    @GetMapping("/balances")
    public List<LeaveBalanceDto> balances(@RequestParam UUID employeeId,
                                          @RequestParam(required = false) LocalDate asOfDate) {
        return service.balances(employeeId, asOfDate);
    }
}
