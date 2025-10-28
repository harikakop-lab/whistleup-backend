package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.resource.OwnerDetailsRequestResource;
import com.whistleup.backend.resource.OwnerDetailsResponseResource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flat_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlatDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long flatId;

    @Column(name = "flat_number")
    private String flatNumber;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "tenant_name")
    private String tenantName;
}
