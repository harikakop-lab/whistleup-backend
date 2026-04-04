package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.VisitorEntryCreateRequest;
import com.whistleup.backend.resource.VisitorEntryResponse;
import com.whistleup.backend.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/whistleup/visitors")
@CrossOrigin("*")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<VisitorEntryResponse>> listByBuildingAndDate(
            @PathVariable("buildingId") Long buildingId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(visitorService.listForBuildingAndDate(buildingId, date));
    }

    @PostMapping("/building/{buildingId}")
    public ResponseEntity<VisitorEntryResponse> create(
            @PathVariable("buildingId") Long buildingId,
            @Valid @RequestBody VisitorEntryCreateRequest request) {
        VisitorEntryResponse created = visitorService.create(buildingId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
