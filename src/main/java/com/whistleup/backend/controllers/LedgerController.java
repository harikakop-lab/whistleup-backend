package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.CreateLedgerRequest;
import com.whistleup.backend.resource.LedgerResponse;
import com.whistleup.backend.resource.UpdateLedgerRequest;
import com.whistleup.backend.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/ledgers")
@CrossOrigin("*")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public LedgerResponse createLedger(@Valid @RequestBody CreateLedgerRequest request) {
        return ledgerService.createLedger(request);
    }

    @GetMapping
    public LedgerResponse getLedger(
            @RequestParam int year,
            @RequestParam String month,
            @RequestParam(required = false) String buildingId) {
        if (buildingId != null && !buildingId.trim().isEmpty()) {
            return ledgerService.getLedgerByYearAndMonthAndBuilding(year, month, buildingId);
        }
        return ledgerService.getLedgerByYearAndMonth(year, month);
    }

    @PutMapping("/{ledgerId}")
    public LedgerResponse updateLedger(@PathVariable Long ledgerId, @Valid @RequestBody UpdateLedgerRequest request) {
        return ledgerService.updateLedger(ledgerId, request);
    }

    @GetMapping("/{ledgerId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long ledgerId) {
        byte[] pdf = ledgerService.generateLedgerPdf(ledgerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Ledger_" + ledgerId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
