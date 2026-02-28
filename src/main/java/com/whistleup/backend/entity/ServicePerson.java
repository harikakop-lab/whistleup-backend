package com.whistleup.backend.entity;

import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "service_persons")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServicePerson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_person_id", updatable = false, nullable = false)
    private UUID servicePersonId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @ElementCollection(targetClass = ServiceOrderType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "service_person_types", joinColumns = @JoinColumn(name = "service_person_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private Set<ServiceOrderType> serviceTypes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "registered_date", nullable = false, updatable = false)
    private LocalDate registeredDate;

    @OneToOne(mappedBy = "servicePerson", fetch = FetchType.LAZY)
    private ServiceOrder currentOrder;

    @PrePersist
    protected void onCreate() {
        this.registeredDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
}