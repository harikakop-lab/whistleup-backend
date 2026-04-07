package com.whistleup.backend.resource;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrBuildingMappingResponse {
    private String token;
    private Long buildingId;
    private String buildingName;
}
