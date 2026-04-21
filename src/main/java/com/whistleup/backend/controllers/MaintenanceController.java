package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.resource.MaintenanceAppliancesOptInResource;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import com.whistleup.backend.service.FileStorageService;
import com.whistleup.backend.service.MaintenanceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
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
    private final FileStorageService fileStorageService;

    @GetMapping("/appliances-opted-in")
    public ResponseEntity<List<MaintenanceAppliancesOptInResource>> listAppliancesOptIn(
            @RequestParam("buildingId") String buildingId) {
        return ResponseEntity.ok(maintenanceService.listAppliancesOptInFlats(buildingId));
    }

    @PostMapping("/create")
    public ResponseEntity<List<MaintenanceResponseResource>> create(@RequestBody MaintenanceCreateResource req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.createOrUpdateMaintenance(req));
    }

    @PatchMapping("/update")
    public ResponseEntity<List<MaintenanceResponseResource>> update(@RequestBody MaintenanceCreateResource req) {
        return ResponseEntity.ok(maintenanceService.updateMaintenance(req));
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

    @PatchMapping(value = "/{id}/pay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> markPaid(
            @PathVariable Long id,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "transactionReference", required = false) String transactionReference,
            @RequestPart(value = "proof", required = false) MultipartFile proof) {
        maintenanceService.markAsPaid(id, paymentMethod, transactionReference, proof);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payment-proof/{maintenanceId}/{fileName}")
    public ResponseEntity<Resource> getPaymentProof(
            @PathVariable Long maintenanceId,
            @PathVariable String fileName,
            HttpServletRequest request) {
        Resource resource = fileStorageService.loadMaintenancePaymentProof(maintenanceId, fileName);

        String contentType;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (Exception ex) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
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

    @GetMapping("/profile/{profileId}/invoice")
    public ResponseEntity<Resource> downloadInvoiceByProfileAndPeriod(
            @PathVariable String profileId,
            @RequestParam String buildingId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Maintenance maintenance = maintenanceService.getInvoiceByProfileAndPeriod(profileId, buildingId, year, month);
        Path path = Paths.get(maintenance.getInvoicePath());
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
