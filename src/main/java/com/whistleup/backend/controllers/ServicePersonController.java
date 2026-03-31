package com.whistleup.backend.controllers;

import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.resource.ServicePersonOnboardRequest;
import com.whistleup.backend.resource.ServicePersonResource;
import com.whistleup.backend.service.ProfileService;
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
    private final ProfileService profileService;

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
            @PathVariable String serviceType) {
        return ResponseEntity.ok(
                servicePersonService.getActiveServicePersonsByType(
                        ServiceOrderType.valueOf(serviceType.trim().toUpperCase())));
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

    /**
     * Admin onboarding: creates/updates both a Profile (for login) and a ServicePerson (for pooling).
     */
    @PostMapping("/onboard")
    public ResponseEntity<ServicePersonResource> onboardServicePerson(
            @Valid @RequestBody ServicePersonOnboardRequest request) {

        String phone = request.getPhoneNumber().trim();

        if (!profileService.doesProfileExists(phone)) {
            profileService.createProfile(ProfileCreateResource.builder()
                    .name(request.getName())
                    .phone(phone)
                    .pin(request.getPlainPin())
                    .role(Roles.SERVICE_PERSON)
                    .isAssigned(true)
                    .build());
        } else {
            ProfileResponseResource existingProfile = profileService.getProfileByUsername(phone);
            if (existingProfile.getRole() != Roles.SERVICE_PERSON) {
                throw new IllegalArgumentException(
                        "Phone is already registered under a different role: " + existingProfile.getRole());
            }

            profileService.updateProfile(ProfileCreateResource.builder()
                    .name(request.getName())
                    .phone(phone)
                    .pin(request.getPlainPin())
                    .role(Roles.SERVICE_PERSON)
                    .isAssigned(true)
                    .buildingId("")
                    .floor("")
                    .flatNo("")
                    .build());
        }

        ServicePersonResource desiredServicePerson = ServicePersonResource.builder()
                .name(request.getName())
                .phoneNumber(phone)
                .address(request.getAddress())
                .experienceYears(request.getExperienceYears())
                .rating(request.getRating() != null ? request.getRating() : "4.8")
                .serviceTypes(request.getServiceTypes())
                .serviceCity(request.getServiceCity())
                .build();

        var existingServicePerson = servicePersonService.findServicePersonByPhoneNumber(phone);
        if (existingServicePerson.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(servicePersonService.createServicePerson(desiredServicePerson));
        }

        return ResponseEntity.ok(
                servicePersonService.updateServicePerson(
                        existingServicePerson.get().getServicePersonId().toString(),
                        desiredServicePerson
                )
        );
    }
}