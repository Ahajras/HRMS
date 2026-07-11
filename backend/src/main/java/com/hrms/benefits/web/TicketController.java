package com.hrms.benefits.web;

import com.hrms.benefits.dto.TicketDtos;
import com.hrms.benefits.service.TicketService;
import com.hrms.migration.dto.ImportSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping("/fares")
    public List<TicketDtos.FareDto> fares() {
        return service.fares();
    }

    @PostMapping("/fares")
    public TicketDtos.FareDto createFare(@RequestBody TicketDtos.FareDto dto) {
        return service.saveFare(dto);
    }

    @PutMapping("/fares/{id}")
    public TicketDtos.FareDto updateFare(@PathVariable UUID id, @RequestBody TicketDtos.FareDto dto) {
        dto.setId(id);
        return service.saveFare(dto);
    }

    @PostMapping("/fares/import")
    public ImportSummary importFares(@RequestParam("file") MultipartFile file) {
        return service.importFares(file);
    }

    @GetMapping("/ledger")
    public List<TicketDtos.LedgerDto> ledger(@RequestParam UUID employeeId) {
        return service.ledger(employeeId);
    }

    @PostMapping("/ledger")
    public TicketDtos.LedgerDto saveLedger(@RequestBody TicketDtos.LedgerDto dto) {
        return service.saveLedger(dto);
    }

    @GetMapping("/balance")
    public TicketDtos.BalanceDto balance(@RequestParam UUID employeeId,
                                         @RequestParam(required = false) LocalDate asOfDate) {
        return service.balance(employeeId, asOfDate);
    }
}
