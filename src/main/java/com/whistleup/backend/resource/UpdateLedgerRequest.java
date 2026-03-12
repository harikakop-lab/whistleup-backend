package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateLedgerRequest {

    @NotNull
    private List<LedgerItemRequest> items;

    @NotNull
    private String buildingId;

    // getters & setters
}
