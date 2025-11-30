package com.whistleup.backend.service.impl;

import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.service.ResidentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ResidentsServiceImpl implements ResidentsService {

    private final ProfileRepository profileRepository;

    public ResidentsServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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
}
