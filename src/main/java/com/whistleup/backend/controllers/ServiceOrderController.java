package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.service.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/service/order")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @GetMapping("/{profileId}/all")
    public ResponseEntity<List<ServiceOrderResource>> getAllOrdersForProfile(
            @PathVariable String profileId) {
        return ResponseEntity.ok(serviceOrderService.getAllOrdersForProfile(profileId));
    }

    @GetMapping("/{profileId}/{orderId}")
    public ResponseEntity<ServiceOrderResource> getOrderByProfileAndOrderId(
            @PathVariable String profileId,
            @PathVariable String orderId) {
        return ResponseEntity.ok(serviceOrderService.getOrderByProfileAndOrderId(profileId, orderId));
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

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteServiceOrder(@PathVariable String orderId) {
        serviceOrderService.deleteServiceOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}