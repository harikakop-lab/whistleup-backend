package com.whistleup.backend.resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterPushTokenRequest {
    private String phone;
    private String pushToken;
    private String platform;
}
