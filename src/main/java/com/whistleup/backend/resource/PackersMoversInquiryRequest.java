package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PackersMoversInquiryRequest {

    @NotBlank
    private String profileId;

    @NotBlank
    private String buildingId;

    private String contactPhone;

    @NotBlank
    private String subcategoryKey;

    @NotNull
    private Map<String, Object> payload;
}
