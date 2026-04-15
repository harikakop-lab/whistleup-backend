package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParkingAllocationResponse {

    private Long id;
    private String displayName;
    private String flatNo;
    private String vehicleType;
    private Boolean guest;
    private String guestRelatedFlatNo;
    private Instant createdAt;
}
