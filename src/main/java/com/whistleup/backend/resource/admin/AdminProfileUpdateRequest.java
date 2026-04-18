package com.whistleup.backend.resource.admin;

import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.resource.ContactResource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminProfileUpdateRequest {

    private String name;
    private String email;
    private String pin;
    private Roles role;
    private String upiId;
    private String buildingId;
    private String floor;
    private String flatNo;
    private Boolean isAssigned;
    private List<ContactResource> contacts;
}
