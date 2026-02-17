package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.whistleup.backend.constants.Roles;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponseResource extends ProfileCreateResource {

    private String userId;
    private String name;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    private Roles role;
    private String avatarUri;
    private String buildingName;
}
