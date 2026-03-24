package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {

    /** Optional when {@code fcmToken} is set. */
    private String expoPushToken;

    /** Optional when {@code expoPushToken} is set. Native FCM token from the device. */
    private String fcmToken;

    @NotBlank
    private String platform; // ANDROID | IOS

    @NotBlank
    private String phone;
}