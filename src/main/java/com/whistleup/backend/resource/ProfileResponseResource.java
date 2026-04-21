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

import java.util.List;

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
    private Boolean isAssigned;

    /** Buildings this user may administer (ADMIN / SYSTEM_ADMIN); null for other roles. */
    private List<AdminBuildingSummaryResource> adminBuildings;

    /** Signed-style download URLs; populated for admin resident detail only. */
    private String idDocumentFrontUri;
    private String idDocumentBackUri;
    private String companyIdDocumentUri;
}
