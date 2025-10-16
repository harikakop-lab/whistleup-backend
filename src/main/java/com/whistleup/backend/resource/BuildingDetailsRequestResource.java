package com.whistleup.backend.resource;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDetailsRequestResource {

    private String buildingName;
    private AddressResource buildingAddress;
}
