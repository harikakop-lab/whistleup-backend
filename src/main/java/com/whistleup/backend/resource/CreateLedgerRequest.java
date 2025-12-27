package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
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
public class CreateLedgerRequest {

    @NotNull
    private Integer year;

    @NotBlank
    private String month;

    @NotNull
    private Integer totalFlats;

    @NotNull
    private List<LedgerItemRequest> items;

    // getters & setters
}
