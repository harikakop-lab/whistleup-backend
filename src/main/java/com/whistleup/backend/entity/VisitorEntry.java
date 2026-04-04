package com.whistleup.backend.entity;

import com.whistleup.backend.constants.VisitorPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "visitor_entry",
        indexes = @Index(name = "idx_visitor_building_visit_at", columnList = "building_id,visit_at")
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitorEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingDetails building;

    @Column(name = "visitor_name", nullable = false, length = 200)
    private String visitorName;

    @Column(name = "visitor_phone", nullable = false, length = 32)
    private String visitorPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 40)
    private VisitorPurpose purpose;

    @Column(name = "visited_flat_no", nullable = false, length = 64)
    private String visitedFlatNo;

    @Column(name = "visit_at", nullable = false)
    private Instant visitAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
