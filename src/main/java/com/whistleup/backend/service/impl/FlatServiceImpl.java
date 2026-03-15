package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.FlatDetails;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.FlatRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.FlatRequestResource;
import com.whistleup.backend.resource.FlatResponseResource;
import com.whistleup.backend.service.FlatService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlatServiceImpl implements FlatService {

    private final FlatRepository flatRepository;

    private final ProfileRepository profileRepository;

    private final BuildingDetailsRepository buildingDetailsRepository;

    public FlatServiceImpl(FlatRepository flatRepository, ProfileRepository profileRepository, BuildingDetailsRepository buildingDetailsRepository) {
        this.flatRepository = flatRepository;
        this.profileRepository = profileRepository;
        this.buildingDetailsRepository = buildingDetailsRepository;
    }

    @Override
    public List<FlatResponseResource> getAllFlats() {
        List<FlatDetails> flatDetailsList = flatRepository.findAll();
        return flatDetailsList.stream().map(flatDetails -> {
            FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
            BeanUtils.copyProperties(flatDetails, flatResponseResource);
            return flatResponseResource;
        }).toList();
    }

    @Override
    public List<String> getAllFlatsByBuildingId(String buildingId) {
        val optionalFlatDetails = flatRepository.findFlatsByBuilding(Long.valueOf(buildingId));
        return optionalFlatDetails.map(flatDetails -> flatDetails.stream().map(FlatDetails::getFlatNumber).collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    @Override
    public FlatResponseResource getFlatDetailsByFlatNumber(String flatNumber) {
        FlatDetails flatDetails = flatRepository.findFlatByFlatNumber(flatNumber).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatNumber));
        FlatResponseResource flatResponseResource = new FlatResponseResource();
        BeanUtils.copyProperties(flatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public FlatResponseResource getFlatDetailsById(Long flatId) {
        FlatDetails flatDetails = flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(flatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public FlatResponseResource addFlatDetails(FlatRequestResource flatRequestResource) {
        FlatDetails flatDetails = FlatDetails.builder().build();
        BeanUtils.copyProperties(flatRequestResource, flatDetails);
        BuildingDetails buildingDetails = buildingDetailsRepository.findById(Long.valueOf(flatRequestResource.getBuildingId())).orElse(null);
        flatDetails.setBuilding(buildingDetails);
        Profile profile = profileRepository.findById(flatRequestResource.getPhone()).orElse(null);
        flatDetails.setResident(profile);
        flatDetails.setFloor(flatRequestResource.getFloorNo());
        FlatDetails savedFlatDetails = flatRepository.save(flatDetails);
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(savedFlatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public FlatResponseResource updateFlateDetails(Long flatId, FlatRequestResource flatRequestResource) {
        FlatDetails flatDetails = flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        FlatDetails flatEntity = FlatDetails.builder().build();
        BeanUtils.copyProperties(flatRequestResource, flatEntity);
        FlatDetails sabedFlatDetails = flatRepository.save(flatEntity);
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(sabedFlatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public void deleteFlatDetails(Long flatId) {
        flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        flatRepository.deleteById(flatId);
    }
}
