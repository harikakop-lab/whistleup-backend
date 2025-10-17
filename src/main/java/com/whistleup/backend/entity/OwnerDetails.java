package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whistleup.backend.resource.AddressResource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "owner_details")
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OwnerDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long ownerId;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "flat_details")
    private String flatDetails;

    @Column(name = "owner_mobile_number")
    private String ownerMobileNumber;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_address")
    private AddressResource addressResource;


}
