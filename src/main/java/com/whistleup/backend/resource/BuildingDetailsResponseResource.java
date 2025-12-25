package com.whistleup.backend.resource;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDetailsResponseResource extends BuildingDetailsRequestResource {

    private Long buildingId;
    private String buildingName;
    private AddressResource buildingAddress;
    private ServiceResource plumbingService;
    private ServiceResource electricService;
    private ServiceResource carpenterService;
    private ServiceResource cleaningService;
    private ServiceResource watchmen;
    private String profileId;
    private Long floors;
}
