package com.whistleup.backend.service;

import com.whistleup.backend.resource.CommunityFeedResponseResource;

public interface CommunityService {
    CommunityFeedResponseResource getFeedByBuilding(String buildingId);
}
