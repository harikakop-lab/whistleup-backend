package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.exception.NotFoundException;
//import com.whistleup.backend.repository.ComplaintImageRepository;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintImageResponse;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import com.whistleup.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComplaintsServiceImpl implements ComplaintsService {

    private final ComplaintsRepository complaintsRepository;

    private final FileStorageService fileStorageService;

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
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByAssigneeProfileId(String profileId) {
        List<Complaints> complaints = complaintsRepository.findByAssigneeProfile(profileId).orElseThrow(() -> new NotFoundException("No Complaints/suggestions found for this given profile id: " + profileId));
        return complaints.stream().map(complaint -> {
            ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
            BeanUtils.copyProperties(complaint, complaintsResponseResource);
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource, MultipartFile[] files) {
        Complaints complaintEntity = Complaints.builder().build();
        BeanUtils.copyProperties(complaintCreateResource, complaintEntity);
        complaintEntity.setProfileId(complaintCreateResource.getUsername());
        Complaints savedEntity = complaintsRepository.save(complaintEntity);
        List<String> imagePaths = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                String savedFileName =
                        fileStorageService.saveComplaintFile(savedEntity.getComplaintId(), file);
                imagePaths.add(savedFileName);
            }
        }

        savedEntity.setImagePaths(imagePaths);
        complaintsRepository.save(savedEntity);
        ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(savedEntity, complaintsResponseResource);
        List<String> imageUrls = imagePaths.stream()
                .map(name -> baseUrl + "/media/complaints/" +
                        savedEntity.getComplaintId() + "/" + name)
                .toList();
        complaintsResponseResource.setImageUrls(imageUrls);
        return complaintsResponseResource;
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
}
