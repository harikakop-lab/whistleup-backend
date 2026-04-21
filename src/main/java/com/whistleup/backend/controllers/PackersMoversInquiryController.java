package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.PackersMoversInquiryRequest;
import com.whistleup.backend.resource.PackersMoversInquiryResponse;
import com.whistleup.backend.resource.PackersMoversItemSectionResource;
import com.whistleup.backend.service.PackersMoversInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/whistleup/packers-movers")
@CrossOrigin("*")
@RequiredArgsConstructor
public class PackersMoversInquiryController {

    private final PackersMoversInquiryService inquiryService;

    @GetMapping("/items")
    public ResponseEntity<List<PackersMoversItemSectionResource>> getItems() {
        return ResponseEntity.ok(inquiryService.getItemSections());
    }

    @PostMapping("/inquiries")
    public ResponseEntity<PackersMoversInquiryResponse> createInquiry(
            @Valid @RequestBody PackersMoversInquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.createInquiry(request));
    }

    @PostMapping("/inquiries/{inquiryId}/video")
    public ResponseEntity<Map<String, Object>> uploadWalkthroughVideo(
            @PathVariable Long inquiryId,
            @RequestParam("file") MultipartFile file) {
        inquiryService.attachWalkthroughVideo(inquiryId, file);
        return ResponseEntity.ok(Map.of(
                "stored", true,
                "inquiryId", inquiryId
        ));
    }
}
