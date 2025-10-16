package com.whistleup.backend.resource;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDetailsResponseResource {

    private Long buildingId;
    private String buildingName;
    private AddressResource buildingAddress;
}
