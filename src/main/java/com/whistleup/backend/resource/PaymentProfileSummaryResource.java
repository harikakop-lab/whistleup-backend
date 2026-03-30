package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentProfileSummaryResource {
    private String profileId;
    private String buildingId;
    private int year;
    private String month;
    private PaymentCurrentResource maintenanceCurrent;
    private List<PaymentCurrentResource> maintenanceHistory;
    private PaymentCurrentResource rentCurrent;
    private List<PaymentCurrentResource> rentHistory;
}
