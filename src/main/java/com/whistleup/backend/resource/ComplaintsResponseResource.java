package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whistleup.backend.constants.ComplaintStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComplaintsResponseResource extends ComplaintCreateResource {
    private Long complaintId;
    private List<String> imageUrls;
    private String raisedBy;
    private String flatNumber;
    private ComplaintStatus status;
    private String createdAt;
    private String updatedAt;
}
