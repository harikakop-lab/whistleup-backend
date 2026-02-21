package com.whistleup.backend.service.impl;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.NotFoundException;
//import com.whistleup.backend.repository.ComplaintImageRepository;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintImageResponse;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import com.whistleup.backend.service.FileStorageService;
import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComplaintsServiceImpl implements ComplaintsService {

    private final ComplaintsRepository complaintsRepository;

    private final FileStorageService fileStorageService;

    private final ProfileRepository profileRepository;

    private final NotificationSendService notificationSendService;

    @Value("${app.base-url}")
    private String baseUrl;

//    private final ComplaintImageRepository imageRepository;

    @Override
    public List<ComplaintsResponseResource> getAllComplaints() {
        List<Complaints> complaints = complaintsRepository.findAll();
        return complaints.stream().map(complaintEntity -> {
            ComplaintsResponseResource complaintsResponseResource = new ComplaintsResponseResource();
            BeanUtils.copyProperties(complaintEntity, complaintsResponseResource);
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public ComplaintsResponseResource getAllComplaintsById(String complaintId) {
        Complaints complaintEntity = complaintsRepository.findById(Long.valueOf(complaintId)).orElseThrow(() -> new NotFoundException("No Complaints/suggestions found with this given id: " + complaintId));
        ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(complaintEntity, complaintsResponseResource);
        return complaintsResponseResource;
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByProfileId(String profileId) {
        List<Complaints> complaints = complaintsRepository.findByProfileId(profileId).orElseThrow(() -> new NotFoundException("No Complaints/suggestions found for this given profile id: " + profileId));
        return complaints.stream().map(complaint -> {
            ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
            BeanUtils.copyProperties(complaint, complaintsResponseResource);
            Optional<Profile> profileOptional = profileRepository.findByPhone(complaint.getProfileId());
            complaintsResponseResource.setRaisedBy(profileOptional.isPresent() ? profileOptional.get().getName() : "");
            complaintsResponseResource.setFlatNumber(profileOptional.isPresent() ? profileOptional.get().getFlatNo() : "");
//            complaintsResponseResource.setImageUrls(complaint.getImagePaths());
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByAssigneeProfileId(String profileId) {
        List<Complaints> complaints = complaintsRepository.findByAssigneeProfile(profileId)
                .orElseThrow(() -> new NotFoundException("No Complaints/suggestions found for profile id: " + profileId));
        return complaints.stream().map(complaint -> {
            ComplaintsResponseResource response = ComplaintsResponseResource.builder().build();
            BeanUtils.copyProperties(complaint, response);
            Optional<Profile> profileOptional = profileRepository.findByPhone(complaint.getProfileId());
            response.setRaisedBy(profileOptional.map(Profile::getName).orElse(""));
            response.setFlatNumber(profileOptional.map(Profile::getFlatNo).orElse(""));

            List<String> imageUrls = new ArrayList<>();
            List<String> imagePaths = new ArrayList<>();

            if (!CollectionUtils.isEmpty(imagePaths)) {
                for (String fileName : imagePaths) {
//                    imageUrls.add(buildComplaintImageUrl(complaint.getComplaintId(), fileName));
                }
            }
            response.setImageUrls(imageUrls);
            return response;
        }).toList();
    }

    @Override
    public ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource, MultipartFile[] files) throws IOException {

        // 1️⃣ Create & save complaint entity
        Complaints complaintEntity = new Complaints();
        BeanUtils.copyProperties(complaintCreateResource, complaintEntity);
        complaintEntity.setProfileId(complaintCreateResource.getUsername());

        Complaints savedEntity = complaintsRepository.save(complaintEntity);
        if (files != null) {
            for (MultipartFile file : files) {
                ComplaintImage image = ComplaintImage.builder()
                        .complaint(savedEntity)
                        .imageData(file.getBytes())
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .build();

                savedEntity.getImages().add(image);
            }
        }

        complaintsRepository.save(savedEntity);
        ComplaintsResponseResource response = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(savedEntity, response);
        List<String> imageUrls = savedEntity.getImages().stream()
                .map(img -> "/issues/" + savedEntity.getComplaintId() +
                        "/images/" + img.getId())
                .toList();

        response.setImageUrls(imageUrls);
        response.setImageUrls(imageUrls);
        val assigneeProfile = Long.valueOf(savedEntity.getAssigneeProfile());
        val title = "A new issue created";
        val body = "Please check in All issues section for more details";
        notificationSendService.notifyUser(assigneeProfile, title, body, IssueType.INFO.name());
        return response;
    }

//    public List<ComplaintImageResponse> getImagesByComplaintId(String complaintId) {
//
//        return imageRepository.findByComplaintId(complaintId)
//                .stream()
//                .map(img -> ComplaintImageResponse.builder()
//                        .imageId(img.getId())
//                        .fileName(img.getFileName())
//                        .contentType(img.getContentType())
//                        .imageUrl("/whistleup/issues/" + img.getId())
//                        .build())
//                .toList();
//    }
//
//    public ComplaintImage getImage(Long imageId) {
//        return imageRepository.findById(imageId)
//                .orElseThrow(() -> new RuntimeException("Image not found"));
//    }

    @Override
    public void deleteComplaint(String complaintId) {
        complaintsRepository.deleteById(Long.valueOf(complaintId));
    }

    @Override
    public void resolveTicket(String complaintId) {
        Complaints complaint = complaintsRepository.findById(Long.valueOf(complaintId))
                .orElseThrow(() -> new NotFoundException("No Complaint found"));
        complaint.setResolved(Boolean.TRUE);
        complaintsRepository.save(complaint);
    }
}
