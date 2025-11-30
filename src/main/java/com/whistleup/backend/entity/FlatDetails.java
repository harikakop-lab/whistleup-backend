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

    // Many flats belong to one building
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingDetails building;

    // One flat mapped to one resident (Profile)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile resident;

    @Column(name = "floor_no")
    private Long floor;
}
