package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollResponseResource {

    private Long pollId;
    private String title;
    private List<String> options;
    private Map<String, Integer> votesPerOption;
    private Integer totalVotes;
    private String timestamp;
    private boolean isClosed;
}
