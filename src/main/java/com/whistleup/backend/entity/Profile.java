package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.whistleup.backend.constants.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "profile")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile {

    @Column(name = "name")
    private String name;

    @Id
    @Column(name = "phone", unique = true, nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "pin")
    private String pin;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Roles role;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "building_id")
    private String buildingId;

    @Column(name = "floor")
    private String floor;

    @Column(name = "flat_no")
    private String flatNo;

    @Column(name = "is_assigned")
    private Boolean isAssigned;

    @Column(name = "id_document_front_path", length = 512)
    private String idDocumentFrontPath;

    @Column(name = "id_document_back_path", length = 512)
    private String idDocumentBackPath;

    @Column(name = "company_id_document_path", length = 512)
    private String companyIdDocumentPath;

    @Column(name = "appliances_json", columnDefinition = "TEXT")
    private String appliancesJson;

    @Column(name = "appliances_maintenance_opt_in")
    private Boolean appliancesMaintenanceOptIn;
}
