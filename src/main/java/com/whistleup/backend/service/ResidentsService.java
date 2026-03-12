package com.whistleup.backend.service;

import com.whistleup.backend.controllers.ResidentsResponse;

import java.util.List;

public interface ResidentsService {

    List<ResidentsResponse> getAllResidentsByBuildingAndFloor(String buildingId, String floorNo);

    List<ResidentsResponse> getAllResidentsByBuilding(String buildingId);
}
