package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.WaterBillRequest;
import com.whistleup.backend.service.WaterBillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/water-bill")
@RequiredArgsConstructor
public class WaterBillController {

    private final WaterBillService waterBillService;

    @PostMapping
    public ResponseEntity<?> createWaterBill(@Valid @RequestBody WaterBillRequest request) {
        waterBillService.createWaterBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
