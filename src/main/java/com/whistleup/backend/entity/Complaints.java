package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whistleup.backend.constants.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class Complaints {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "complaint_id")
    private Long complaintId;

    @Column(name = "username")
    private String username;

    @Column(name = "profileId")
    private String profileId;

    @Column(name = "is_assigned")
    private boolean isAssigned;

    @Column(name = "type")
    private String type;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "is_resolved")
    private boolean isResolved;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplaintStatus status;

    @Column(name = "assignee_profile")
    private String assigneeProfile;

    @Column(name = "building_id")
    private String buildingId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintImage> images = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ComplaintStatus.OPEN;
        }
        if (this.status == ComplaintStatus.RESOLVED) {
            this.isResolved = true;
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.isResolved = this.status == ComplaintStatus.RESOLVED;
    }
}
