package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.resource.BuildingServices;
import com.whistleup.backend.resource.ServiceResource;
import com.whistleup.backend.service.BuildingDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BuildingDetailsServiceImpl implements BuildingDetailsService {

    private final BuildingDetailsRepository buildingDetailsRepository;

    public BuildingDetailsServiceImpl(BuildingDetailsRepository buildingDetailsRepository) {
        this.buildingDetailsRepository = buildingDetailsRepository;
    }

    @Override
    public List<BuildingDetailsResponseResource> getExistingBuildingsInformation() {
        List<BuildingDetails> buildingDetailsList = buildingDetailsRepository.findAll();
        return buildingDetailsList.stream().map(buildingDetails -> {
            BuildingDetailsResponseResource buildingDetailsResponseResource = new BuildingDetailsResponseResource();
            BeanUtils.copyProperties(buildingDetails, buildingDetailsResponseResource);
            return buildingDetailsResponseResource;
        }).toList();
    }

    @Override
    public BuildingDetails saveBuildingDetails(BuildingDetailsRequestResource buildingDetailsRequestResource) {
        BuildingDetails buildingDetails = convertToBuildingDetails(buildingDetailsRequestResource);
        return buildingDetailsRepository.save(buildingDetails);
    }

    private BuildingDetails convertToBuildingDetails(BuildingDetailsRequestResource buildingDetailsRequestResource) {
        BuildingDetails buildingDetails = new BuildingDetails();
        BeanUtils.copyProperties(buildingDetailsRequestResource, buildingDetails);
        return buildingDetails;
    }

    @Override
    public BuildingDetails getBuildingDetails(Long buildingId) {
        return buildingDetailsRepository.findById(buildingId).orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
    }

    @Override
    public BuildingDetails updateBuildingDetails(Long buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource) {
        BuildingDetails existingBuildingDetails = buildingDetailsRepository.findById(buildingId)
                .orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
        existingBuildingDetails.setBuildingName(updateBuildingDetailsRequestResource.getBuildingName());
        existingBuildingDetails.setBuildingAddress(updateBuildingDetailsRequestResource.getBuildingAddress());
        return buildingDetailsRepository.save(existingBuildingDetails);
    }

    @Override
    public BuildingDetails updateBuildingAddress(Long buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource) {
        BuildingDetails existingBuildingDetails = buildingDetailsRepository.findById(buildingId)
                .orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
        existingBuildingDetails.setBuildingAddress(updateBuildingDetailsRequestResource.getBuildingAddress());
        return buildingDetailsRepository.save(existingBuildingDetails);
    }

    @Override
    public void deleteBuildingDetails(Long buildingId) {
        buildingDetailsRepository.findById(buildingId)
                .orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
        buildingDetailsRepository.deleteById(buildingId);
    }

    @Override
    public BuildingDetails getBuildingServicesByProfileId(String username) {
        return buildingDetailsRepository.findByProfileId(username)
                .orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
    }

    @Override
    public List<BuildingDetailsResponseResource> getBuildingDetailsDropDown() {
        List<BuildingDetails> buildingDetailsList = buildingDetailsRepository.findAll();
        return buildingDetailsList.stream().map(buildingDetails -> {
            BuildingDetailsResponseResource buildingDetailsResponseResource = new BuildingDetailsResponseResource();
            buildingDetailsResponseResource.setBuildingId(buildingDetails.getBuildingId());
            buildingDetailsResponseResource.setBuildingName(buildingDetails.getBuildingName());
            buildingDetailsResponseResource.setFloors(buildingDetails.getFloors());
            return buildingDetailsResponseResource;
        }).toList();
    }

    @Override
    public void updateBuildingServices(Long buildingId, BuildingServices buildingServices) {
        BuildingDetails buildingDetails = buildingDetailsRepository.findById(buildingId).orElseThrow(() -> new NotFoundException("No Buildings found with this id"));
        if (Objects.nonNull(buildingServices.getWatchmen())) {
            buildingDetails.setWatchmen(buildingServices.getWatchmen());
        }
        if (Objects.nonNull(buildingServices.getCarpenterService())) {
            buildingDetails.setCarpenterService(buildingServices.getCarpenterService());
        }
        if (Objects.nonNull(buildingServices.getCleaningService())) {
            buildingDetails.setCleaningService(buildingServices.getCarpenterService());
        }
        if (Objects.nonNull(buildingServices.getElectricService())) {
            buildingDetails.setElectricService(buildingServices.getElectricService());
        }
        if (Objects.nonNull(buildingServices.getPlumbingService())) {
            buildingDetails.setPlumbingService(buildingServices.getPlumbingService());
        }
        buildingDetailsRepository.save(buildingDetails);
    }
}
