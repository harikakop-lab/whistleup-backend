package com.whistleup.backend.controllers;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.service.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/whistleup/service/order")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @GetMapping("/{profileId}/all")
    public ResponseEntity<List<ServiceOrderResource>> getAllOrdersForProfile(
            @PathVariable String profileId,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) ServiceIssueStatus issueStatus) {
        return ResponseEntity.ok(serviceOrderService.getAllOrdersForProfile(profileId, orderStatus, issueStatus));
    }

    @GetMapping("/{profileId}/{orderId}")
    public ResponseEntity<ServiceOrderResource> getOrderByProfileAndOrderId(
            @PathVariable String profileId,
            @PathVariable String orderId) {
        return ResponseEntity.ok(serviceOrderService.getOrderByProfileAndOrderId(profileId, orderId));
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ServiceOrderResource>> getAllOrdersForBuilding(
            @PathVariable String buildingId) {
        return ResponseEntity.ok(serviceOrderService.getAllOrdersForBuilding(buildingId));
    }

    @PostMapping("/create")
    public ResponseEntity<ServiceOrderResource> createServiceOrder(
            @Valid @RequestBody ServiceOrderResource createResource) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceOrderService.createServiceOrder(createResource));
    }

    @PatchMapping("/{orderId}/assign/{servicePersonId}")
    public ResponseEntity<ServiceOrderResource> assignServicePerson(
            @PathVariable String orderId,
            @PathVariable String servicePersonId) {
        return ResponseEntity.ok(serviceOrderService.assignServicePerson(orderId, servicePersonId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ServiceOrderResource> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("orderStatus");
        if (status == null) {
            throw new IllegalArgumentException("orderStatus is required");
        }
        return ResponseEntity.ok(
                serviceOrderService.updateOrderStatus(orderId, OrderStatus.valueOf(status.trim().toUpperCase())));
    }

    @PatchMapping("/{orderId}/issue")
    public ResponseEntity<ServiceOrderResource> raiseIssue(
            @PathVariable String orderId,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(serviceOrderService.raiseIssue(orderId, payload.get("issueText")));
    }

    @PatchMapping("/{orderId}/issue/status")
    public ResponseEntity<ServiceOrderResource> updateIssueStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("issueStatus");
        if (status == null) {
            throw new IllegalArgumentException("issueStatus is required");
        }
        return ResponseEntity.ok(
                serviceOrderService.updateIssueStatus(orderId, ServiceIssueStatus.valueOf(status.trim().toUpperCase())));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteServiceOrder(@PathVariable String orderId) {
        serviceOrderService.deleteServiceOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}