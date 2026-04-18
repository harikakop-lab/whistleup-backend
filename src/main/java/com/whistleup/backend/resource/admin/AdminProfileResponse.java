package com.whistleup.backend.resource.admin;

import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.resource.ContactResource;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminProfileResponse {

    private String phone;
    private String name;
    private String email;
    private Roles role;
    private String upiId;
    private String buildingId;
    private String floor;
    private String flatNo;
    private Boolean isAssigned;
    private List<ContactResource> contacts;
}
