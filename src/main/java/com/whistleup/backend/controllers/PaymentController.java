package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.PaymentApartmentSummaryResource;
import com.whistleup.backend.resource.PaymentCurrentResource;
import com.whistleup.backend.resource.PaymentProfileSummaryResource;
import com.whistleup.backend.resource.RentUpsertRequest;
import com.whistleup.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/payments")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<PaymentProfileSummaryResource> getProfileSummary(
            @PathVariable String profileId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month
    ) {
        return ResponseEntity.ok(paymentService.getProfileSummary(profileId, year, month));
    }

    @GetMapping("/apartment")
    public ResponseEntity<PaymentApartmentSummaryResource> getApartmentSummary(
            @RequestParam String buildingId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month
    ) {
        return ResponseEntity.ok(paymentService.getApartmentSummary(buildingId, year, month));
    }

    @PostMapping("/rent")
    public ResponseEntity<PaymentCurrentResource> upsertRent(@RequestBody RentUpsertRequest request) {
        return ResponseEntity.ok(paymentService.upsertRent(request));
    }

    @PatchMapping("/rent/{rentId}/pay")
    public ResponseEntity<Void> payRent(@PathVariable Long rentId) {
        paymentService.markRentPaid(rentId);
        return ResponseEntity.ok().build();
    }
}
