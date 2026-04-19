package com.whistleup.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.entity.PackersMoversInquiry;
import com.whistleup.backend.repository.PackersMoversInquiryRepository;
import com.whistleup.backend.resource.PackersMoversInquiryRequest;
import com.whistleup.backend.resource.PackersMoversInquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PackersMoversInquiryService {

    private static final long MAX_VIDEO_SIZE_BYTES = 200L * 1024 * 1024;

    private final PackersMoversInquiryRepository inquiryRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public PackersMoversInquiryResponse createInquiry(PackersMoversInquiryRequest request) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payload provided");
        }

        PackersMoversInquiry inquiry = PackersMoversInquiry.builder()
                .profileId(request.getProfileId().trim())
                .buildingId(request.getBuildingId().trim())
                .contactPhone(StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone().trim() : null)
                .subcategoryKey(request.getSubcategoryKey().trim())
                .payloadJson(payloadJson)
                .status("NEW")
                .build();

        PackersMoversInquiry saved = inquiryRepository.save(inquiry);
        return PackersMoversInquiryResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public void attachWalkthroughVideo(Long inquiryId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }
        if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new IllegalArgumentException("Video size exceeds 200MB limit");
        }
        if (!isValidVideoType(file)) {
            throw new IllegalArgumentException("Only MP4 and MOV videos are allowed");
        }
        PackersMoversInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));
        String storedFileName = fileStorageService.savePackersMoversWalkthrough(inquiryId, file);
        inquiry.setWalkthroughVideoFileName(storedFileName);
        inquiryRepository.save(inquiry);
    }

    private boolean isValidVideoType(MultipartFile file) {
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType().trim().toLowerCase()
                : "";
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim().toLowerCase()
                : "";
        return "video/mp4".equals(contentType)
                || "video/quicktime".equals(contentType)
                || fileName.endsWith(".mp4")
                || fileName.endsWith(".mov");
    }
}
