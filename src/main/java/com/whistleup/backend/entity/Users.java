package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id")
    private String userId;

    @Column(name =  "name")
    private String name;

    @Column(name = "user_email_or_phone", nullable = false)
    private String emailOrPhoneNumber;

    @Column(name = "password", nullable = false)
    @Size(min = 6, message = "assword must be at least 6 characters ")
    private String password;

}
