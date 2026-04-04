package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitorEntryResponse {

    private Long id;
    private String visitorName;
    private String visitorPhone;
    /** Same string as {@link com.whistleup.backend.constants.VisitorPurpose} name */
    private String purpose;
    private String visitedFlatNo;
    private Instant visitAt;
    private String notes;
}
