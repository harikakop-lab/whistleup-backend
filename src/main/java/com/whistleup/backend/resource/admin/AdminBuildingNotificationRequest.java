package com.whistleup.backend.resource.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBuildingNotificationRequest {

    @NotBlank
    private String buildingId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String type;
}
