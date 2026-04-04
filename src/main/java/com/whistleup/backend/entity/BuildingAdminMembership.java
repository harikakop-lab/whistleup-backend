package com.whistleup.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "building_admin_membership",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "admin_phone"})
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingAdminMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", referencedColumnName = "id", nullable = false)
    private BuildingDetails building;

    @Column(name = "admin_phone", nullable = false, length = 32)
    private String adminPhone;
}
