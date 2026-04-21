package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.whistleup.backend.constants.Roles;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileCreateResource {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String pin;
    @Enumerated(EnumType.STRING)
    private Roles role;
    private List<ContactResource> contacts;
    private String upiId;
    private String buildingId;
    private String floor;
    private String flatNo;
    private Boolean isAssigned;

    /** JSON array of appliance rows: `[{"type":"Refrigerator"},{"type":"OTHER","customLabel":"..."}]` */
    private String appliancesJson;

    /** When true, resident is included in appliance maintenance split for the building. */
    private Boolean appliancesMaintenanceOptIn;
}
