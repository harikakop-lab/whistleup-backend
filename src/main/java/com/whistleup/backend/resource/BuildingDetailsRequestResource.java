package com.whistleup.backend.resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDetailsRequestResource {

    @NotEmpty
    private String buildingName;
    @Valid
    @NotNull
    private AddressResource buildingAddress;
    private Long floors;
    private Long flatStartNumber;
    private Long flatEndNumber;
    private Long totalFlats;
    private ServiceResource plumbingService;
    private ServiceResource electricService;
    private ServiceResource carpenterService;
    private ServiceResource cleaningService;
    private ServiceResource watchmen;
    private String profileId;
    private String adminName;
    private String adminPhone;
    private String adminEmail;
    private Long totalResidents;
    private String upiId;
    private boolean isWaterBillRequired;
}
