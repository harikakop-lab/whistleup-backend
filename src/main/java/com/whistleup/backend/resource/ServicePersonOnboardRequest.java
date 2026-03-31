package com.whistleup.backend.resource;

import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePersonOnboardRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phoneNumber;

    /**
     * Plain 4-digit PIN. The backend will bcrypt-hash it when creating the profile.
     */
    @NotBlank
    private String plainPin;

    @NotBlank
    private String address;

    @NotNull
    private Integer experienceYears;

    private String rating;

    @NotBlank
    private String serviceCity;

    @NotNull
    private Set<ServiceOrderType> serviceTypes;
}

