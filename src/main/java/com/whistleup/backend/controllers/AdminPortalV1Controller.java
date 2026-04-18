package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.resource.admin.AdminBuildingNotificationRequest;
import com.whistleup.backend.resource.admin.AdminComplaintStatusUpdateRequest;
import com.whistleup.backend.resource.admin.AdminProfileResponse;
import com.whistleup.backend.resource.admin.AdminProfileUpdateRequest;
import com.whistleup.backend.service.admin.AdminPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/whistleup/admin-portal/v1")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminPortalV1Controller {

    private final AdminPortalService adminPortalService;

    @GetMapping("/service-orders")
    public ResponseEntity<Page<ServiceOrderResource>> getServiceOrders(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPortalService.getServiceOrders(q, page, size));
    }

    @GetMapping("/service-orders/{orderId}")
    public ResponseEntity<ServiceOrderResource> getServiceOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminPortalService.getServiceOrder(orderId));
    }

    @GetMapping("/complaints")
    public ResponseEntity<Page<ComplaintsResponseResource>> getComplaints(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPortalService.getComplaints(q, page, size));
    }

    @GetMapping("/complaints/{complaintId}")
    public ResponseEntity<ComplaintsResponseResource> getComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(adminPortalService.getComplaint(complaintId));
    }

    @PatchMapping("/complaints/{complaintId}/status")
    public ResponseEntity<ComplaintsResponseResource> updateComplaintStatus(
            @PathVariable Long complaintId,
            @Valid @RequestBody AdminComplaintStatusUpdateRequest request) {
        return ResponseEntity.ok(adminPortalService.updateComplaintStatus(complaintId, request.getStatus()));
    }

    @GetMapping("/complaints/images/{imageId}/download")
    public ResponseEntity<byte[]> downloadComplaintImage(@PathVariable Long imageId) {
        ComplaintImage image = adminPortalService.getComplaintImage(imageId);
        String filename = image.getFileName() == null || image.getFileName().isBlank()
                ? "complaint-image-" + imageId
                : image.getFileName();
        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(filename).build();
        MediaType mediaType = image.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.getContentType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(image.getImageData());
    }

    @PostMapping("/notifications/building")
    public ResponseEntity<Map<String, Object>> sendNotificationToBuilding(
            @Valid @RequestBody AdminBuildingNotificationRequest request) {
        int recipients = adminPortalService.sendNotificationToBuilding(request);
        return ResponseEntity.ok(Map.of(
                "message", "Notification queued",
                "recipients", recipients
        ));
    }

    @GetMapping("/profiles")
    public ResponseEntity<Page<AdminProfileResponse>> getProfiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPortalService.getProfiles(q, buildingId, page, size));
    }

    @GetMapping("/profiles/{phone}")
    public ResponseEntity<AdminProfileResponse> getProfile(@PathVariable String phone) {
        return ResponseEntity.ok(adminPortalService.getProfile(phone));
    }

    @PutMapping("/profiles/{phone}")
    public ResponseEntity<AdminProfileResponse> updateProfile(
            @PathVariable String phone,
            @RequestBody AdminProfileUpdateRequest request) {
        return ResponseEntity.ok(adminPortalService.updateProfile(phone, request));
    }

    @GetMapping("/buildings")
    public ResponseEntity<Page<BuildingDetailsResponseResource>> getBuildings(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPortalService.getBuildings(q, page, size));
    }

    @GetMapping("/buildings/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> getBuilding(@PathVariable Long buildingId) {
        return ResponseEntity.ok(adminPortalService.getBuilding(buildingId));
    }

    @PutMapping("/buildings/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> updateBuilding(
            @PathVariable Long buildingId,
            @RequestBody BuildingDetailsRequestResource request) {
        return ResponseEntity.ok(adminPortalService.updateBuilding(buildingId, request));
    }
}
