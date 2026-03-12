package com.whistleup.backend.controllers;

//import com.whistleup.backend.entity.ComplaintImage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.repository.ComplaintImageRepository;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintImageResponse;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/whistleup/issues")
@CrossOrigin("*")
public class ComplaintsController {

    private final ComplaintsService complaintsService;

    private final ComplaintsRepository complaintsRepository;

    private final ComplaintImageRepository complaintImageRepository;

    public ComplaintsController(ComplaintsService complaintsService, ComplaintsRepository complaintsRepository, ComplaintImageRepository complaintImageRepository) {
        this.complaintsService = complaintsService;
        this.complaintsRepository = complaintsRepository;
        this.complaintImageRepository = complaintImageRepository;
    }

    @GetMapping("")
    public ResponseEntity<List<ComplaintsResponseResource>> getAllComplaints() {
        List<ComplaintsResponseResource> complaints = complaintsService.getAllComplaints();
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ComplaintsResponseResource> getComplaint(@PathVariable("complaintId") String complaintId) {
        ComplaintsResponseResource complaints = complaintsService.getAllComplaintsById(complaintId);
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @PutMapping("/{complaintId}")
    public ResponseEntity<Void> markComplaintAsResolved(@PathVariable("complaintId") String complaintId) {
        complaintsService.resolveTicket(complaintId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<List<ComplaintsResponseResource>> getAllComplaintsRaisedByProfileId(@PathVariable("profileId") String profileId) {
        List<ComplaintsResponseResource> complaints = complaintsService.getComplaintsByProfileId(profileId);
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @GetMapping("/assignee/{profileId}")
    public ResponseEntity<List<ComplaintsResponseResource>> getAllComplaintsAssignedToProfileId(@PathVariable("profileId") String profileId) {
        List<ComplaintsResponseResource> complaints = complaintsService.getComplaintsByAssigneeProfileId(profileId);
        return new ResponseEntity<>(complaints, HttpStatus.OK);
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintsResponseResource> registerComplaint(@RequestPart("complaint") String complaintJson,
        @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        ComplaintCreateResource complaint =
                new ObjectMapper().readValue(complaintJson, ComplaintCreateResource.class);
        ComplaintsResponseResource response =
                complaintsService.registerComplaint(complaint, files);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{complaintId}/images")
    public ResponseEntity<List<String>> getImageUrls(
            @PathVariable Long complaintId) {
        Complaints complaint = complaintsRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        List<String> urls = complaint.getImages()
                .stream()
                .map(img -> "/issues/images/" + img.getId())
                .toList();
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
        ComplaintImage image = complaintImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getImageData());
    }

//    @GetMapping("/{complaintId}/images")
//    public ResponseEntity<List<ComplaintImageResponse>> getImagesByComplaintId(
//            @PathVariable String complaintId) {
//        return ResponseEntity.ok(complaintsService.getImagesByComplaintId(complaintId));
//    }
//
//    @GetMapping("/images/{imageId}")
//    public ResponseEntity<byte[]> getImageById(@PathVariable Long imageId) {
//
//        ComplaintImage image = complaintsService.getImage(imageId);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(image.getContentType()))
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "inline; filename=\"" + image.getFileName() + "\"")
//                .body(image.getImageData());
//    }


    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable String complaintId) {
        complaintsService.deleteComplaint(complaintId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
