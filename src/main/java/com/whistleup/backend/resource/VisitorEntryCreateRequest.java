package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisitorEntryCreateRequest {

    @NotBlank
    private String visitorName;

    @NotBlank
    private String visitorPhone;

    /** Enum name e.g. FOOD_DELIVERY */
    @NotBlank
    private String purpose;

    @NotBlank
    private String visitedFlatNo;

    /** If omitted, server uses current time (IST wall-clock instant). */
    private Instant visitAt;

    /** Optional free-text from visitor check-in form. */
    private String notes;
}
