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
    public ResponseEntity<List<ComplaintCreateResource>> getAllComplaints() {
        complaintsService.getAllComplaints();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ComplaintCreateResource> getAllComplaints(@PathVariable String complaintId) {
        complaintsService.getAllComplaintsById(complaintId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ComplaintCreateResource> getAllComplaintsAssignedToProfileId(@PathVariable String profileId,
                                                                                       @RequestParam("isAssigned") boolean isAssigned) {
        complaintsService.getComplaintsByProfileId(profileId, isAssigned);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ComplaintCreateResource> registerComplaint(@RequestBody ComplaintCreateResource complaintCreateResource) {
        ComplaintsResponseResource complaintsResponseResource = complaintsService.registerComplaint(complaintCreateResource);
        return new ResponseEntity<>(complaintsResponseResource, HttpStatus.CREATED);
    }

    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable String complaintId) {
        complaintsService.deleteComplaint(complaintId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
