package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParkingAllocationCreateRowRequest {

    private String flatNo;

    private String vehicleType;

    private Boolean guest;

    private String guestRelatedFlatNo;
}
