package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.NotebookBalanceResponse;
import com.whistleup.backend.resource.NotebookResponse;
import com.whistleup.backend.resource.NotebookUpsertRequest;
import com.whistleup.backend.service.NotebookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/notebook")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NotebookController {

    private final NotebookService notebookService;

    @PostMapping("/upsert")
    public ResponseEntity<NotebookResponse> upsertNotebook(@Valid @RequestBody NotebookUpsertRequest request) {
        return ResponseEntity.ok(notebookService.upsertNotebook(request));
    }

    @GetMapping
    public ResponseEntity<NotebookResponse> getNotebook(
            @RequestParam String buildingId,
            @RequestParam Integer year,
            @RequestParam String month
    ) {
        return notebookService.tryGetNotebookForPeriod(buildingId, year, month)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/previous-balance")
    public ResponseEntity<NotebookBalanceResponse> getPreviousBalance(
            @RequestParam String buildingId,
            @RequestParam Integer year,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(notebookService.getPreviousMonthBalance(buildingId, year, month));
    }
}
