package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingAdminMembership;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.repository.BuildingAdminMembershipRepository;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.resource.AdminBuildingSummaryResource;
import com.whistleup.backend.service.BuildingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuildingAdminServiceImpl implements BuildingAdminService {

    private final BuildingAdminMembershipRepository membershipRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;

    @Override
    public List<AdminBuildingSummaryResource> resolveAdminBuildings(String adminPhone, String profileBuildingIdOptional) {
        if (adminPhone == null || adminPhone.isBlank()) {
            return List.of();
        }
        String phone = adminPhone.trim();
        Map<Long, AdminBuildingSummaryResource> byId = new LinkedHashMap<>();

        for (BuildingAdminMembership m : membershipRepository.findByAdminPhoneTrimmed(phone)) {
            BuildingDetails b = m.getBuilding();
            if (b != null && b.getBuildingId() != null) {
                putSummary(byId, b);
            }
        }

        for (BuildingDetails b : buildingDetailsRepository.findByTrimmedAdminPhone(phone)) {
            if (b != null && b.getBuildingId() != null) {
                putSummary(byId, b);
            }
        }

        if (profileBuildingIdOptional != null && !profileBuildingIdOptional.isBlank()) {
            try {
                Long pid = Long.valueOf(profileBuildingIdOptional.trim());
                Optional<BuildingDetails> opt = buildingDetailsRepository.findById(pid);
                opt.ifPresent(b -> putSummary(byId, b));
            } catch (NumberFormatException ignored) {
                // ignore invalid profile building id
            }
        }

        List<AdminBuildingSummaryResource> list = new ArrayList<>(byId.values());
        list.sort(Comparator
                .comparing((AdminBuildingSummaryResource r) -> r.getBuildingName() == null ? "" : r.getBuildingName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> r.getBuildingId() == null ? "" : r.getBuildingId()));
        return list;
    }

    private static void putSummary(Map<Long, AdminBuildingSummaryResource> byId, BuildingDetails b) {
        Long id = b.getBuildingId();
        if (id == null) {
            return;
        }
        byId.putIfAbsent(id, AdminBuildingSummaryResource.builder()
                .buildingId(String.valueOf(id))
                .buildingName(b.getBuildingName() != null ? b.getBuildingName() : "")
                .build());
    }
}
