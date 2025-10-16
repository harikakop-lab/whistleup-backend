package com.whistleup.backend.service;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;

public interface BuildingDetailsService {

    BuildingDetails saveBuildingDetails(BuildingDetailsRequestResource buildingDetailsRequestResource);

    BuildingDetails getBuildingDetails(String buildingId);

    BuildingDetails updateBuildingDetails(String buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource);

    BuildingDetails updateBuildingAddress(String buildingId, BuildingDetailsRequestResource updateBuildingDetailsRequestResource);

    void deleteBuildingDetails(String buildingId);

}
