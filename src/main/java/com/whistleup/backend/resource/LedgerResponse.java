package com.whistleup.backend.resource;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerResponse {

    private Long id;
    private String buildingId;
    private int year;
    private String month;
    private double totalAmount;
    private int totalFlats;
    private double perFlatAmount;
    private Long flatsPaid;
    private LocalDate dueDate;
    private List<LedgerItemResponse> items;

    public LedgerResponse(Long id, int year, String month,
                          double totalAmount, int totalFlats,
                          double perFlatAmount,
                          List<LedgerItemResponse> items) {
        this.id = id;
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
        this.totalFlats = totalFlats;
        this.perFlatAmount = perFlatAmount;
        this.items = items;
    }

    // getters
}
