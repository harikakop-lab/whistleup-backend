package com.whistleup.backend.service.impl;

import com.whistleup.backend.constants.ParkingVehicleType;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.ParkingAllocation;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.ParkingAllocationRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.ParkingAllocationCreateBatchRequest;
import com.whistleup.backend.resource.ParkingAllocationCreateRowRequest;
import com.whistleup.backend.resource.ParkingAllocationResponse;
import com.whistleup.backend.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingAllocationRepository parkingAllocationRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParkingAllocationResponse> listByBuilding(Long buildingId) {
        ensureBuildingExists(buildingId);
        List<ParkingAllocation> allocations = parkingAllocationRepository
                .findByBuilding_BuildingIdOrderByCreatedAtDesc(buildingId);
        Map<String, String> flatToName = resolveFlatToNameMap(String.valueOf(buildingId), allocations);
        return allocations.stream()
                .map(allocation -> toResponse(allocation, flatToName.get(allocation.getFlatNo())))
                .toList();
    }

    @Override
    @Transactional
    public List<ParkingAllocationResponse> createAllocations(Long buildingId, ParkingAllocationCreateBatchRequest request) {
        BuildingDetails building = buildingDetailsRepository.findById(buildingId)
                .orElseThrow(() -> new NotFoundException("Building not found: " + buildingId));
        List<ParkingAllocationCreateRowRequest> rows = request != null ? request.getRows() : null;
        if (rows == null || rows.isEmpty()) {
            throw new BadRequestException("At least one parking allocation row is required");
        }

        List<ParkingAllocation> newAllocations = rows.stream()
                .map(row -> toEntity(building, row))
                .toList();

        List<ParkingAllocation> saved = parkingAllocationRepository.saveAll(newAllocations);
        Map<String, String> flatToName = resolveFlatToNameMap(String.valueOf(buildingId), saved);
        return saved.stream()
                .map(allocation -> toResponse(allocation, flatToName.get(allocation.getFlatNo())))
                .toList();
    }

    private ParkingAllocation toEntity(BuildingDetails building, ParkingAllocationCreateRowRequest row) {
        if (row == null) {
            throw new BadRequestException("Allocation row cannot be empty");
        }
        String flatNo = normalizeRequired(row.getFlatNo(), "flatNo");
        ParkingVehicleType vehicleType = ParkingVehicleType.fromRaw(row.getVehicleType());
        boolean guest = Boolean.TRUE.equals(row.getGuest());
        String guestRelatedFlatNo = normalizeOptional(row.getGuestRelatedFlatNo());
        if (guest && guestRelatedFlatNo == null) {
            throw new BadRequestException("guestRelatedFlatNo is required for guest parking");
        }
        if (!guest) {
            guestRelatedFlatNo = null;
        }

        return ParkingAllocation.builder()
                .building(building)
                .flatNo(flatNo)
                .vehicleType(vehicleType)
                .guestParking(guest)
                .guestRelatedFlatNo(guestRelatedFlatNo)
                .build();
    }

    private ParkingAllocationResponse toResponse(ParkingAllocation allocation, String displayName) {
        return ParkingAllocationResponse.builder()
                .id(allocation.getId())
                .displayName((displayName == null || displayName.isBlank()) ? "-" : displayName)
                .flatNo(allocation.getFlatNo())
                .vehicleType(allocation.getVehicleType().name())
                .guest(allocation.isGuestParking())
                .guestRelatedFlatNo(allocation.getGuestRelatedFlatNo())
                .createdAt(allocation.getCreatedAt())
                .build();
    }

    private Map<String, String> resolveFlatToNameMap(String buildingId, List<ParkingAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> flats = allocations.stream()
                .map(ParkingAllocation::getFlatNo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .toList();
        if (flats.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Profile> profiles = profileRepository.findByBuildingIdAndFlatNoIn(buildingId, flats);
        Map<String, String> flatToName = new LinkedHashMap<>();
        for (Profile profile : profiles) {
            String flat = normalizeOptional(profile.getFlatNo());
            String name = normalizeOptional(profile.getName());
            if (flat != null && name != null && !flatToName.containsKey(flat)) {
                flatToName.put(flat, name);
            }
        }
        return flatToName;
    }

    private void ensureBuildingExists(Long buildingId) {
        if (!buildingDetailsRepository.existsById(buildingId)) {
            throw new NotFoundException("Building not found: " + buildingId);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
