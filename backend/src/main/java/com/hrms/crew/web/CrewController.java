package com.hrms.crew.web;

import com.hrms.crew.dto.BulkCrewMemberRequest;
import com.hrms.crew.dto.CrewDto;
import com.hrms.crew.dto.CrewMemberDto;
import com.hrms.crew.dto.CrewTradeDto;
import com.hrms.crew.service.CrewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** REST API for crews and their members (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/crews")
public class CrewController {

    private final CrewService service;

    public CrewController(CrewService service) {
        this.service = service;
    }

    @GetMapping
    public List<CrewDto> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrewDto create(@Valid @RequestBody CrewDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public CrewDto update(@PathVariable UUID id, @Valid @RequestBody CrewDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-employee/{employeeId}")
    public CrewDto byEmployee(@PathVariable UUID employeeId) {
        return service.findByEmployee(employeeId);
    }

    @GetMapping("/{id}/members")
    public List<CrewMemberDto> members(@PathVariable UUID id) {
        return service.listMembers(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public CrewMemberDto addMember(@PathVariable UUID id, @RequestBody CrewMemberDto dto) {
        return service.addMember(id, dto);
    }

    @PostMapping("/{id}/members/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> bulkAddMembers(@PathVariable UUID id, @RequestBody BulkCrewMemberRequest req) {
        return Map.of("created", service.bulkAddMembers(id, req));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID memberId) {
        service.removeMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/trades")
    public List<CrewTradeDto> trades(@PathVariable UUID id) {
        return service.listTrades(id);
    }

    @PostMapping("/{id}/trades")
    @ResponseStatus(HttpStatus.CREATED)
    public CrewTradeDto addTrade(@PathVariable UUID id, @RequestBody CrewTradeDto dto) {
        return service.addTrade(id, dto);
    }

    @DeleteMapping("/trades/{tradeId}")
    public ResponseEntity<Void> removeTrade(@PathVariable UUID tradeId) {
        service.removeTrade(tradeId);
        return ResponseEntity.noContent().build();
    }
}
