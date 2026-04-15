package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ParkingAllocationCreateBatchRequest;
import com.whistleup.backend.resource.ParkingAllocationResponse;
import com.whistleup.backend.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/whistleup/parking")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ParkingAllocationResponse>> listByBuilding(
            @PathVariable("buildingId") Long buildingId) {
        return ResponseEntity.ok(parkingService.listByBuilding(buildingId));
    }

    @PostMapping("/building/{buildingId}/allocations")
    public ResponseEntity<List<ParkingAllocationResponse>> createAllocations(
            @PathVariable("buildingId") Long buildingId,
            @RequestBody ParkingAllocationCreateBatchRequest request) {
        List<ParkingAllocationResponse> created = parkingService.createAllocations(buildingId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
