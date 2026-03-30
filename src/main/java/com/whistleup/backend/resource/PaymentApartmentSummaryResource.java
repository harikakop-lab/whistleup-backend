package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentApartmentSummaryResource {
    private String buildingId;
    private int year;
    private String month;
    private double totalCollection;
    private double collected;
    private double pending;
    private double spent;
    private Long flatsPaid;
    private int flatsTotal;
    private double perFlatAmount;
    private LocalDate dueDate;
    private List<LedgerItemResponse> items;
}
