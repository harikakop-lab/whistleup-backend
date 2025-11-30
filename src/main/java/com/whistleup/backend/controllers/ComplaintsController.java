package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/issues")
@CrossOrigin("*")
public class ComplaintsController {

    private final ComplaintsService complaintsService;

    public ComplaintsController(ComplaintsService complaintsService) {
        this.complaintsService = complaintsService;
    }

    @GetMapping("")
    public ResponseEntity<List<ComplaintsResponseResource>> getAllComplaints() {
        List<ComplaintsResponseResource> complaints = complaintsService.getAllComplaints();
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ComplaintsResponseResource> getAllComplaints(@PathVariable("complaintId") String complaintId) {
        ComplaintsResponseResource complaints = complaintsService.getAllComplaintsById(complaintId);
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<List<ComplaintsResponseResource>> getAllComplaintsAssignedToProfileId(@PathVariable("profileId") String profileId) {
        List<ComplaintsResponseResource> complaints = complaintsService.getComplaintsByProfileId(profileId);
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ComplaintsResponseResource> registerComplaint(@RequestBody ComplaintCreateResource complaintCreateResource) {
        ComplaintsResponseResource complaintsResponseResource = complaintsService.registerComplaint(complaintCreateResource);
        return new ResponseEntity<>(complaintsResponseResource, HttpStatus.CREATED);
    }

    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable String complaintId) {
        complaintsService.deleteComplaint(complaintId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
