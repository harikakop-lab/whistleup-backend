package com.whistleup.backend.service;

import com.whistleup.backend.resource.AdminBuildingSummaryResource;

import java.util.List;

public interface BuildingAdminService {

    List<AdminBuildingSummaryResource> resolveAdminBuildings(String adminPhone, String profileBuildingIdOptional);
}
