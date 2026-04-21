package com.whistleup.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.entity.PackersMoversInquiry;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.repository.PackersMoversInquiryRepository;
import com.whistleup.backend.repository.ServiceOrderRepository;
import com.whistleup.backend.resource.PackersMoversItemRowResource;
import com.whistleup.backend.resource.PackersMoversItemSectionResource;
import com.whistleup.backend.resource.PackersMoversInquiryRequest;
import com.whistleup.backend.resource.PackersMoversInquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PackersMoversInquiryService {

    private static final long MAX_VIDEO_SIZE_BYTES = 200L * 1024 * 1024;

    private final PackersMoversInquiryRepository inquiryRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public List<PackersMoversItemSectionResource> getItemSections() {
        return List.of(
                PackersMoversItemSectionResource.builder()
                        .key("living")
                        .title("Living room")
                        .rows(List.of(
                                PackersMoversItemRowResource.builder().key("sofa").label("Sofa").build(),
                                PackersMoversItemRowResource.builder().key("tv-unit").label("TV unit").build()
                        ))
                        .build(),
                PackersMoversItemSectionResource.builder()
                        .key("bedroom")
                        .title("Bedroom")
                        .rows(List.of(
                                PackersMoversItemRowResource.builder().key("double-bed").label("Double bed").build(),
                                PackersMoversItemRowResource.builder().key("wardrobe").label("Wardrobe").build()
                        ))
                        .build(),
                PackersMoversItemSectionResource.builder()
                        .key("kitchen")
                        .title("Kitchen")
                        .rows(List.of(
                                PackersMoversItemRowResource.builder().key("fridge").label("Refrigerator").build(),
                                PackersMoversItemRowResource.builder().key("rice-cooker").label("Rice Cooker").build()
                        ))
                        .build()
        );
    }

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
        saveServiceOrderFromInquiryRequest(request, saved.getPayloadJson());
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

    private void saveServiceOrderFromInquiryRequest(PackersMoversInquiryRequest request, String payloadJson) {
        LocalDate serviceDate = resolveServiceDate(request.getPayload());
        String optionTitle = resolveServiceTitle(request.getPayload(), request.getSubcategoryKey());
        String slot = asText(request.getPayload().get("slot"));
        String orderNotes = buildOrderNotes(request, optionTitle, slot);

        ServiceOrder order = ServiceOrder.builder()
                // Keep DB-compatible enum value; packers identity is retained in optionId/optionTitle/notes.
                .orderType(ServiceOrderType.CLEANER)
                .profileId(request.getProfileId().trim())
                .buildingId(request.getBuildingId().trim())
                .date(serviceDate)
                .timeSlot(slot)
                .optionId(request.getSubcategoryKey().trim())
                .optionTitle(optionTitle)
                .notes(orderNotes)
                .amount(null)
                .vhsBookingId(null)
                .vhsStatus(null)
                .vhsServicePersonName(null)
                .vhsServicePersonPhone(null)
                .orderStatus(OrderStatus.CREATED)
                .build();

        serviceOrderRepository.save(order);
    }

    @SuppressWarnings("unchecked")
    private LocalDate resolveServiceDate(Map<String, Object> payload) {
        Object dateObj = payload.get("date");
        if (dateObj instanceof Map<?, ?> dateMapObj) {
            Map<String, Object> dateMap = (Map<String, Object>) dateMapObj;
            String isoDate = asText(dateMap.get("isoDate"));
            if (isoDate != null && !isoDate.isBlank()) {
                try {
                    return OffsetDateTime.parse(isoDate).toLocalDate();
                } catch (RuntimeException ignored) {
                    // fall through to default below
                }
            }
        }
        return LocalDate.now().plusDays(1);
    }

    @SuppressWarnings("unchecked")
    private String resolveServiceTitle(Map<String, Object> payload, String fallbackSubcategory) {
        Object serviceObj = payload.get("service");
        if (serviceObj instanceof Map<?, ?> serviceMapObj) {
            Map<String, Object> serviceMap = (Map<String, Object>) serviceMapObj;
            String title = asText(serviceMap.get("title"));
            if (title != null && !title.isBlank()) {
                return title.trim();
            }
        }
        return fallbackSubcategory == null ? "Packers & Movers" : fallbackSubcategory.trim();
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    @SuppressWarnings("unchecked")
    private String buildOrderNotes(PackersMoversInquiryRequest request, String optionTitle, String slot) {
        Map<String, Object> payload = request.getPayload();
        int itemsCount = 0;
        Object itemsObj = payload.get("items");
        if (itemsObj instanceof List<?> sections) {
            for (Object sectionObj : sections) {
                if (!(sectionObj instanceof Map<?, ?> sectionMapObj)) continue;
                Map<String, Object> sectionMap = (Map<String, Object>) sectionMapObj;
                Object rowsObj = sectionMap.get("rows");
                if (!(rowsObj instanceof List<?> rows)) continue;
                for (Object rowObj : rows) {
                    if (!(rowObj instanceof Map<?, ?> rowMapObj)) continue;
                    Map<String, Object> rowMap = (Map<String, Object>) rowMapObj;
                    Object countObj = rowMap.get("count");
                    if (countObj instanceof Number number) {
                        itemsCount += Math.max(0, number.intValue());
                    }
                }
            }
        }

        String phone = asText(request.getContactPhone());
        String summary = "Packers request | service=" + optionTitle
                + " | slot=" + (slot == null ? "--" : slot)
                + " | items=" + itemsCount
                + " | phone=" + (phone == null ? "--" : phone);
        return summary.length() > 240 ? summary.substring(0, 240) : summary;
    }
}
