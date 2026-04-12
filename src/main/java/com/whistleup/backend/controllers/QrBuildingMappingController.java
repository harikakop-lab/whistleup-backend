package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.QrBuildingMappingResponse;
import com.whistleup.backend.resource.QrBuildingMappingUpsertRequest;
import com.whistleup.backend.service.QrBuildingMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/qr-mappings")
@CrossOrigin("*")
@RequiredArgsConstructor
public class QrBuildingMappingController {

    private final QrBuildingMappingService qrBuildingMappingService;

    @PostMapping
    public ResponseEntity<QrBuildingMappingResponse> upsert(
            @Valid @RequestBody QrBuildingMappingUpsertRequest request) {
        QrBuildingMappingResponse response = qrBuildingMappingService.upsert(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{token}")
    public ResponseEntity<QrBuildingMappingResponse> getByToken(
            @PathVariable("token") String token) {
        return ResponseEntity.ok(qrBuildingMappingService.getByToken(token));
    }
}
