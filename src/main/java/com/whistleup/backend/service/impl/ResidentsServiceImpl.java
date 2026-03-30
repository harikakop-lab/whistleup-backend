package com.whistleup.backend.service.impl;

import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.service.ResidentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ResidentsServiceImpl implements ResidentsService {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;

    public ResidentsServiceImpl(ProfileRepository profileRepository, ProfileService profileService) {
        this.profileRepository = profileRepository;
        this.profileService = profileService;
    }

    @Override
    public List<ResidentsResponse> getAllResidentsByBuildingAndFloor(String buildingId, String floorNo) {
        try {
            return profileRepository.getListOfResidents(Long.valueOf(buildingId), Long.valueOf(floorNo));
        } catch (Exception exception) {
            log.error("Error while fetching residents: {}", exception.getMessage(), exception);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ResidentsResponse> getAllResidentsByBuilding(String buildingId) {
        try {
            return profileRepository.getListOfResidentsByBuilding(Long.valueOf(buildingId));
        } catch (Exception exception) {
            log.error("Error while fetching residents: {}", exception.getMessage(), exception);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ResidentsResponse> getPendingResidentsByBuilding(String buildingId) {
        try {
            return profileRepository.getPendingResidentsByBuilding(
                    buildingId,
                    List.of(Roles.USER, Roles.OWNER)
            );
        } catch (Exception exception) {
            log.error("Error while fetching pending residents: {}", exception.getMessage(), exception);
        }
        return Collections.emptyList();
    }

    @Override
    public void approveResident(String phone, String flatNo) {
        if (flatNo == null || flatNo.trim().isEmpty()) {
            throw new BadRequestException("Flat number is required for approval.");
        }
        ProfileCreateResource resource = ProfileCreateResource.builder()
                .phone(phone)
                .flatNo(flatNo.trim())
                .isAssigned(true)
                .build();
        profileService.updateProfile(resource);
    }

    @Override
    public void rejectResident(String phone) {
        profileService.deleteProfile(phone);
    }
}
