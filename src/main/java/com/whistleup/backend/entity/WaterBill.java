package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "water_bills")
public class WaterBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buildingId;

    private Integer year;
    private String month;

    private Double liftCurrentBill;
    private Double commonCurrentBill;
    private Double garbage;
    private Double watchmanSalary;
    private Double miscellaneousExpenses;
    private Double liftMotorMaintenance;
    private Double otherExpenses;

    @OneToMany(
            mappedBy = "waterBill",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WaterReading> waterReadings;
}
