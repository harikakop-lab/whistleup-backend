package com.whistleup.backend.controllers;

import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.resource.ServicePersonResource;
import com.whistleup.backend.service.ServicePersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/service/person")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ServicePersonController {

    private final ServicePersonService servicePersonService;

    @GetMapping("/all")
    public ResponseEntity<List<ServicePersonResource>> getAllServicePersons() {
        return ResponseEntity.ok(servicePersonService.getAllServicePersons());
    }

    @GetMapping("/{servicePersonId}")
    public ResponseEntity<ServicePersonResource> getServicePersonById(
            @PathVariable String servicePersonId) {
        return ResponseEntity.ok(servicePersonService.getServicePersonById(servicePersonId));
    }

    @GetMapping("/type/{serviceType}")
    public ResponseEntity<List<ServicePersonResource>> getActiveByServiceType(
            @PathVariable ServiceOrderType serviceType) {
        return ResponseEntity.ok(servicePersonService.getActiveServicePersonsByType(serviceType));
    }

    @PostMapping("/create")
    public ResponseEntity<ServicePersonResource> createServicePerson(
            @Valid @RequestBody ServicePersonResource resource) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicePersonService.createServicePerson(resource));
    }

    @PutMapping("/{servicePersonId}")
    public ResponseEntity<ServicePersonResource> updateServicePerson(
            @PathVariable String servicePersonId,
            @Valid @RequestBody ServicePersonResource resource) {
        return ResponseEntity.ok(servicePersonService.updateServicePerson(servicePersonId, resource));
    }

    @DeleteMapping("/{servicePersonId}")
    public ResponseEntity<Void> deactivateServicePerson(@PathVariable String servicePersonId) {
        servicePersonService.deactivateServicePerson(servicePersonId);
        return ResponseEntity.noContent().build();
    }
}