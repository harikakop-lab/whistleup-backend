package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerItemResponse {

    private Long id;
    private String name;
    private double amount;
    private String sectionKey;
    private String sectionLabel;

    public LedgerItemResponse(Long id, String name, double amount, String sectionKey, String sectionLabel) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.sectionKey = sectionKey;
        this.sectionLabel = sectionLabel;
    }
}
