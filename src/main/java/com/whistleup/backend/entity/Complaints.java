package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Complaints {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "complaint_id")
    private String complaintId;

    @Column(name = "username")
    private String username;

    @Column(name = "type")
    private String type;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;
}
