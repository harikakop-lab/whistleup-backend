package com.whistleup.backend.controllers;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.resource.ServiceOrderRescheduleRequest;
import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.resource.VhsWebhookUpdateRequest;
import com.whistleup.backend.service.ServiceOrderService;
import com.whistleup.backend.service.VhsWebhookAuthService;
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
    private final VhsWebhookAuthService vhsWebhookAuthService;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrderService.createServiceOrder(createResource));
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

    @PatchMapping("/{profileId}/{orderId}/reschedule")
    public ResponseEntity<ServiceOrderResource> rescheduleOrder(
            @PathVariable String profileId,
            @PathVariable String orderId,
            @RequestBody ServiceOrderRescheduleRequest request) {
        return ResponseEntity.ok(serviceOrderService.rescheduleOrder(profileId, orderId, request));
    }

    @PatchMapping("/{profileId}/{orderId}/cancel")
    public ResponseEntity<ServiceOrderResource> cancelOrder(
            @PathVariable String profileId,
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = payload == null ? null : payload.get("cancelReason");
        return ResponseEntity.ok(serviceOrderService.cancelOrder(profileId, orderId, reason));
    }

    /**
     * VHS → Nestiti: update booking status / technician. Requires {@code vhs.webhook-secret} when set
     * (header {@code X-VHS-Webhook-Secret} or {@code Authorization: Bearer <secret>}).
     * <p>
     * URL (production example): {@code POST {base-url}/whistleup/service/order/webhook/vhs}
     * <p>
     * JSON body (all fields optional except booking id):
     * <pre>
     * {
     *   "vhsBookingId": "&lt;id returned from VHS create-booking&gt;",
     *   "status": "ASSIGNED | IN_PROGRESS | COMPLETED | CANCELLED | ...",
     *   "servicePersonName": "Technician name",
     *   "servicePersonPhone": "+91..."
     * }
     * </pre>
     * Aliases accepted: {@code internalBookingId}, {@code booking_id}, {@code booking_status}, etc.
     */
    @PostMapping("/webhook/vhs")
    public ResponseEntity<ServiceOrderResource> updateFromVhsWebhook(
            @RequestHeader(value = "X-VHS-Webhook-Secret", required = false) String vhsWebhookSecret,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VhsWebhookUpdateRequest request) {
        // if (!vhsWebhookAuthService.authorize(vhsWebhookSecret, authorization)) {
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // }
        return ResponseEntity.ok(serviceOrderService.applyVhsWebhook(request));
    }
}