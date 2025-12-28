package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class Complaints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "assignee_profile")
    private String assigneeProfile;

    @ElementCollection
    @CollectionTable(
            name = "complaint_images",
            joinColumns = @JoinColumn(name = "complaint_id")
    )
    @Column(name = "image_path")
    private List<String> imagePaths;

}
