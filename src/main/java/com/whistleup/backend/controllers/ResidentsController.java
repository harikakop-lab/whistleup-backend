package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.resource.ResidentApprovalRequest;
import com.whistleup.backend.service.JwtService;
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.service.ResidentsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/residents")
@CrossOrigin("*")
public class ResidentsController {

    private final ResidentsService residentsService;

    private final ProfileService profileService;

    private final JwtService jwtService;

    public ResidentsController(
            ResidentsService residentsService,
            ProfileService profileService,
            JwtService jwtService) {
        this.residentsService = residentsService;
        this.profileService = profileService;
        this.jwtService = jwtService;
    }

    @GetMapping("/building/{buildingId}/floor/{floorNo}")
    public ResponseEntity<List<ResidentsResponse>> getAllResidentsByBuildingAndFloor(@PathVariable("buildingId") String buildingId, @PathVariable("floorNo") String floorNo) {
        List<ResidentsResponse> listOfResidents = residentsService.getAllResidentsByBuildingAndFloor(buildingId, floorNo);
        return new ResponseEntity<>(listOfResidents, HttpStatus.OK);
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ResidentsResponse>> getAllResidentsByBuildingId(@PathVariable("buildingId") String buildingId) {
        List<ResidentsResponse> listOfResidents = residentsService.getAllResidentsByBuilding(buildingId);
        return new ResponseEntity<>(listOfResidents, HttpStatus.OK);
    }

    @GetMapping("/building/{buildingId}/pending")
    public ResponseEntity<List<ResidentsResponse>> getPendingResidentsByBuilding(@PathVariable("buildingId") String buildingId) {
        List<ResidentsResponse> pendingResidents = residentsService.getPendingResidentsByBuilding(buildingId);
        return new ResponseEntity<>(pendingResidents, HttpStatus.OK);
    }

    @GetMapping("/building/{buildingId}/profile/{phone}")
    public ResponseEntity<ProfileResponseResource> getResidentAdminDetail(
            @PathVariable("buildingId") String buildingId,
            @PathVariable("phone") String phone,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String requester = extractRequester(authHeader);
        ProfileResponseResource body = profileService.getResidentAdminDetail(buildingId, phone, requester);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @PatchMapping("/{phone}/approve")
    public ResponseEntity<Void> approveResident(@PathVariable("phone") String phone,
                                                @RequestBody ResidentApprovalRequest request) {
        residentsService.approveResident(phone, request.getFlatNo());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{phone}/reject")
    public ResponseEntity<Void> rejectResident(@PathVariable("phone") String phone) {
        residentsService.rejectResident(phone);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String extractRequester(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            return jwtService.extractToken(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
