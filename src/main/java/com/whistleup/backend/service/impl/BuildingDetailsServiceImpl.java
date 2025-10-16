package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.service.BuildingDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BuildingDetailsServiceImpl implements BuildingDetailsService {

    private final BuildingDetailsRepository buildingDetailsRepository;

    public BuildingDetailsServiceImpl(BuildingDetailsRepository buildingDetailsRepository) {
        this.buildingDetailsRepository = buildingDetailsRepository;
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
}
