package com.whistleup.backend.service.impl;

import com.whistleup.backend.constants.ComplaintStatus;
import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComplaintsServiceImpl implements ComplaintsService {

    private final ComplaintsRepository complaintsRepository;

    private final ProfileRepository profileRepository;

    private final NotificationSendService notificationSendService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public List<ComplaintsResponseResource> getAllComplaints() {
        return complaintsRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Complaints::getComplaintId).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ComplaintsResponseResource getAllComplaintsById(String complaintId) {
        Complaints complaintEntity = complaintsRepository.findById(Long.valueOf(complaintId))
                .orElseThrow(() -> new NotFoundException("No complaints found with id: " + complaintId));
        return toResponse(complaintEntity);
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByProfileId(String profileId, String buildingId) {
        return complaintsRepository.findByProfileIdAndBuildingIdOrderByComplaintIdDesc(profileId, buildingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByAssigneeProfileId(String profileId, String buildingId) {
        return complaintsRepository.findByAssigneeProfileAndBuildingIdOrderByComplaintIdDesc(profileId, buildingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByBuildingId(String buildingId) {
        return complaintsRepository.findByBuildingIdOrderByComplaintIdDesc(buildingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource, MultipartFile[] files) throws IOException {
        Complaints complaintEntity = new Complaints();
        BeanUtils.copyProperties(complaintCreateResource, complaintEntity);
        complaintEntity.setProfileId(complaintCreateResource.getUsername());
        complaintEntity.setBuildingId(complaintCreateResource.getBuildingId());
        complaintEntity.setStatus(Objects.requireNonNullElse(complaintCreateResource.getStatus(), ComplaintStatus.OPEN));
        complaintEntity.setResolved(complaintEntity.getStatus() == ComplaintStatus.RESOLVED);

        Complaints savedEntity = complaintsRepository.save(complaintEntity);
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    continue;
                }
                ComplaintImage image = ComplaintImage.builder()
                        .complaint(savedEntity)
                        .imageData(file.getBytes())
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .build();

                savedEntity.getImages().add(image);
            }
        }

        Complaints persisted = complaintsRepository.save(savedEntity);
        maybeSendAssigneeNotification(persisted);
        return toResponse(persisted);
    }

    @Override
    public void deleteComplaint(String complaintId) {
        complaintsRepository.deleteById(Long.valueOf(complaintId));
    }

    @Override
    public void resolveTicket(String complaintId) {
        Complaints complaint = complaintsRepository.findById(Long.valueOf(complaintId))
                .orElseThrow(() -> new NotFoundException("No Complaint found"));
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolved(Boolean.TRUE);
        complaintsRepository.save(complaint);
    }

    @Override
    public ComplaintsResponseResource updateStatus(String complaintId, ComplaintStatus status) {
        Complaints complaint = complaintsRepository.findById(Long.valueOf(complaintId))
                .orElseThrow(() -> new NotFoundException("No Complaint found"));
        complaint.setStatus(status);
        complaint.setResolved(status == ComplaintStatus.RESOLVED);
        return toResponse(complaintsRepository.save(complaint));
    }

    private ComplaintsResponseResource toResponse(Complaints complaint) {
        ComplaintsResponseResource response = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(complaint, response);
        Optional<Profile> profileOptional = profileRepository.findByPhone(complaint.getProfileId());
        response.setRaisedBy(profileOptional.map(Profile::getName).orElse(""));
        response.setFlatNumber(profileOptional.map(Profile::getFlatNo).orElse(""));
        response.setStatus(Objects.requireNonNullElse(
                complaint.getStatus(),
                complaint.isResolved() ? ComplaintStatus.RESOLVED : ComplaintStatus.OPEN
        ));
        response.setCreatedAt(complaint.getCreatedAt() != null ? complaint.getCreatedAt().toString() : null);
        response.setUpdatedAt(complaint.getUpdatedAt() != null ? complaint.getUpdatedAt().toString() : null);
        response.setImageUrls(buildImageUrls(complaint));
        return response;
    }

    private List<String> buildImageUrls(Complaints complaint) {
        if (CollectionUtils.isEmpty(complaint.getImages())) {
            return List.of();
        }
        return complaint.getImages()
                .stream()
                .map(img -> baseUrl + "/whistleup/issues/images/" + img.getId())
                .toList();
    }

    private void maybeSendAssigneeNotification(Complaints complaint) {
        if (complaint.getAssigneeProfile() == null || complaint.getAssigneeProfile().trim().isEmpty()) {
            return;
        }
        try {
            Long assigneeProfile = Long.valueOf(complaint.getAssigneeProfile());
            notificationSendService.notifyUser(
                    assigneeProfile,
                    "A new issue created",
                    "Please check in All issues section for more details",
                    IssueType.INFO.name()
            );
        } catch (Exception ex) {
            log.warn("Skipping assignee push notification for complaintId={}", complaint.getComplaintId(), ex);
        }
    }
}
