package com.whistleup.backend.entity;

import com.whistleup.backend.resource.AddressResource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.List;

@Entity
@Table(name = "building_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long buildingId;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "building_address")
    @Embedded
    private AddressResource buildingAddress;

    @Column(name = "created_by")
    @CreatedBy
    private String createdBy;

    @Column(name = "created_date")
    @CreatedDate
    private String createdDate;

    @Column(name = "last_updated_by")
    @LastModifiedBy
    private String last_updatedBy;

    @Column(name = "last_updated_date")
    @LastModifiedDate
    private String lastUpdatedDate;
}
