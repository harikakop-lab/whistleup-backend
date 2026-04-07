package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrBuildingMappingUpsertRequest {

    @NotBlank(message = "token is required")
    private String token;

    @NotNull(message = "buildingId is required")
    private Long buildingId;
}
