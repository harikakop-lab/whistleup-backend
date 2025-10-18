package com.whistleup.backend.service;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;

import java.util.List;

public interface BuildingDetailsService {

    List<BuildingDetailsResponseResource> getExistingBuildingsInformation();

    BuildingDetails saveBuildingDetails(BuildingDetailsRequestResource buildingDetailsRequestResource);

    BuildingDetails getBuildingDetails(Long buildingId);

    BuildingDetails updateBuildingDetails(Long buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource);

    BuildingDetails updateBuildingAddress(Long buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource);

    void deleteBuildingDetails(Long buildingId);

}
