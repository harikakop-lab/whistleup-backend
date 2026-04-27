package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebugNotifyAllRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String body;
}
