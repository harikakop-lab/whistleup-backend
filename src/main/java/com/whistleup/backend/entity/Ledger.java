package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ledgers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ledger_year", "ledger_month"})
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ledger_year", nullable = false)
    private int year;

    @Column(name = "ledger_month", nullable = false)
    private String month;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "total_flats", nullable = false)
    private int totalFlats;

    @Column(name = "per_flat_amount", nullable = false)
    private double perFlatAmount;

    @OneToMany(
            mappedBy = "ledger",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LedgerItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ---------- Lifecycle hooks ---------- */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* ---------- getters & setters ---------- */
}
