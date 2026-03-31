package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.whistleup.backend.constants.ServiceOrderType;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderPoolItemResource {

    private Long orderId;
    private ServiceOrderType orderType;

    /**
     * Category label for the UI (e.g. "Cleaning").
     */
    private String category;

    /**
     * Subcategory label for the UI (comes from optionTitle).
     */
    private String subcategory;

    private String description;

    private String location;

    private LocalDate date;
    private String timeSlot;

    private String buildingName;

    private String bookingPersonName;
    private String bookingPersonPhone;

    private String optionId;
    private Integer amount;
}

