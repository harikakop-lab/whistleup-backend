package com.whistleup.backend.controllers;

import com.whistleup.backend.config.SupportContactProperties;
import com.whistleup.backend.resource.SupportContactResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/support")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SupportContactController {

    private final SupportContactProperties supportContactProperties;

    @GetMapping("/contact")
    public ResponseEntity<SupportContactResource> getSupportContact() {
        String email = nullToEmpty(supportContactProperties.getEmail());
        String phone = nullToEmpty(supportContactProperties.getPhone());
        return ResponseEntity.ok(new SupportContactResource(email, phone));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
