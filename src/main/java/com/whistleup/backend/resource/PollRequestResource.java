package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollRequestResource {

    @NotEmpty
    private String buildingId;

    @NotEmpty
    private String title;

    @NotEmpty
    private List<String> options;

    private Map<String, Integer> votesPerOption;

    @Builder.Default
    private boolean isActive = Boolean.TRUE;
}
