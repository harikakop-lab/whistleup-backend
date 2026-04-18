package com.whistleup.backend.resource.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminComplaintStatusUpdateRequest {

    @NotBlank
    private String status;
}
