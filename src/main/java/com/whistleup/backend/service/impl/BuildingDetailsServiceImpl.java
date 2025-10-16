package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.service.BuildingDetailsService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BuildingDetailsServiceImpl implements BuildingDetailsService {


    @Autowired
    private BuildingDetailsRepository buildingDetailsRepository;

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
    public BuildingDetails getBuildingDetails(String buildingId) {
        return buildingDetailsRepository.findById(buildingId).orElseThrow( () -> new RuntimeException("No Buildings found with this id"));
    }

    @Override
    public BuildingDetails updateBuildingDetails(String buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource) {
        BuildingDetails existingBuildingDetails = buildingDetailsRepository.findById(buildingId).orElseThrow( () -> new RuntimeException("No Buildings found with this id"));
        existingBuildingDetails.setBuildingName(updateBuildingDetailsRequestResource.getBuildingName());
        existingBuildingDetails.setBuildingAddress(updateBuildingDetailsRequestResource.getBuildingAddress());
        return buildingDetailsRepository.save(existingBuildingDetails);
    }

    @Override
    public BuildingDetails updateBuildingAddress(String buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource) {
        BuildingDetails existingBuildingDetails = buildingDetailsRepository.findById(buildingId).orElseThrow( () -> new RuntimeException("No Buildings found with this id"));
        existingBuildingDetails.setBuildingAddress(updateBuildingDetailsRequestResource.getBuildingAddress());
        return buildingDetailsRepository.save(existingBuildingDetails);
    }

    @Override
    public void deleteBuildingDetails(String buildingId) {
        BuildingDetails buildingDetails = buildingDetailsRepository.findById(buildingId).orElseThrow( () -> new RuntimeException("No Buildings found with this id"));
        buildingDetailsRepository.deleteById(buildingId);
    }
}
