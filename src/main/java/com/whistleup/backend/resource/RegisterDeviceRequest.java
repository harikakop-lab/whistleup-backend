package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {

    @NotBlank
    private String expoPushToken;

    @NotBlank
    private String platform; // ANDROID | IOS

    @NotBlank
    private String phone;
}