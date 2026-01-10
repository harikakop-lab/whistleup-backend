package com.whistleup.backend.entity;

import com.whistleup.backend.entity.converter.ServiceResourceConverter;
import com.whistleup.backend.resource.AddressResource;
import com.whistleup.backend.resource.ServiceResource;
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

    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(name = "building_address", nullable = false)
    @Embedded
    private AddressResource buildingAddress;

    @Column(name = "plumbing_service")
    @Convert(converter = ServiceResourceConverter.class)
    private ServiceResource plumbingService;

    @Column(name = "electric_service")
    @Convert(converter = ServiceResourceConverter.class)
    private ServiceResource electricService;

    @Column(name = "cleaning_service")
    @Convert(converter = ServiceResourceConverter.class)
    private ServiceResource cleaningService;

    @Column(name = "carpenter_service")
    @Convert(converter = ServiceResourceConverter.class)
    private ServiceResource carpenterService;

    @Column(name = "watchmen")
    @Convert(converter = ServiceResourceConverter.class)
    private ServiceResource watchmen;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "admin_phone")
    private String adminPhone;

    @Column(name = "admin_name")
    private String adminName;

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

    @Column(name = "floors")
    private Long floors;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "total_residents")
    private Long totalResidents;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "water_bill_required")
    private boolean isWaterBillRequired;
}
