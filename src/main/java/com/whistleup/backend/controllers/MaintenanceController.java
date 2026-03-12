package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import com.whistleup.backend.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/whistleup/maintenance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/create")
    public ResponseEntity<List<MaintenanceResponseResource>> create(@RequestBody MaintenanceCreateResource req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.createOrUpdateMaintenance(req));
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<MaintenanceResponseResource>> getAllMaintenanceByBuilding(@PathVariable String buildingId) {
        return ResponseEntity.ok(maintenanceService.getByBuilding(buildingId));
    }

    @GetMapping("/building/{buildingId}/period")
    public ResponseEntity<List<MaintenanceResponseResource>> getMaintenanceByBuildingAndPeriod(
            @PathVariable String buildingId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(maintenanceService.getByBuildingAndPeriod(buildingId, year, month));
    }


    @GetMapping("/{username}")
    public ResponseEntity<List<MaintenanceResponseResource>> getAllMaintenanceByProfileId(@PathVariable String username) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceByProfileId(username));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<Void> markPaid(@PathVariable Long id) {
        maintenanceService.markAsPaid(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable Long id) {
        Maintenance m = maintenanceService.getEntity(id);

        Path path = Paths.get(m.getInvoicePath());
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + path.getFileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
