package com.whistleup.backend.service.impl;

import com.whistleup.backend.constants.VisitorPurpose;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.VisitorEntry;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.VisitorEntryRepository;
import com.whistleup.backend.resource.VisitorEntryCreateRequest;
import com.whistleup.backend.resource.VisitorEntryResponse;
import com.whistleup.backend.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorServiceImpl implements VisitorService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final VisitorEntryRepository visitorEntryRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VisitorEntryResponse> listForBuildingAndDate(Long buildingId, LocalDate date) {
        ensureBuildingExists(buildingId);
        LocalDate d = date != null ? date : LocalDate.now(IST);
        Instant start = d.atStartOfDay(IST).toInstant();
        Instant end = d.plusDays(1).atStartOfDay(IST).toInstant();
        return visitorEntryRepository
                .findByBuilding_BuildingIdAndVisitAtGreaterThanEqualAndVisitAtLessThanOrderByVisitAtDesc(
                        buildingId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VisitorEntryResponse create(Long buildingId, VisitorEntryCreateRequest request) {
        BuildingDetails building = buildingDetailsRepository
                .findById(buildingId)
                .orElseThrow(() -> new NotFoundException("Building not found: " + buildingId));

        VisitorPurpose purpose = parsePurpose(request.getPurpose());
        Instant visitAt = request.getVisitAt() != null ? request.getVisitAt() : Instant.now();

        String notes = request.getNotes() != null ? request.getNotes().trim() : "";
        if (notes.isEmpty()) {
            notes = null;
        }

        VisitorEntry entry = VisitorEntry.builder()
                .building(building)
                .visitorName(request.getVisitorName().trim())
                .visitorPhone(request.getVisitorPhone().trim())
                .purpose(purpose)
                .visitedFlatNo(request.getVisitedFlatNo().trim())
                .visitAt(visitAt)
                .notes(notes)
                .build();
        VisitorEntry saved = visitorEntryRepository.save(entry);
        return toResponse(saved);
    }

    private void ensureBuildingExists(Long buildingId) {
        if (!buildingDetailsRepository.existsById(buildingId)) {
            throw new NotFoundException("Building not found: " + buildingId);
        }
    }

    private static VisitorPurpose parsePurpose(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("purpose is required");
        }
        try {
            return VisitorPurpose.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid purpose: " + raw);
        }
    }

    private VisitorEntryResponse toResponse(VisitorEntry e) {
        return VisitorEntryResponse.builder()
                .id(e.getId())
                .visitorName(e.getVisitorName())
                .visitorPhone(e.getVisitorPhone())
                .purpose(e.getPurpose().name())
                .visitedFlatNo(e.getVisitedFlatNo())
                .visitAt(e.getVisitAt())
                .notes(e.getNotes())
                .build();
    }
}
