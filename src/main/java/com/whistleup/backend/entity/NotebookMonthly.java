package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.whistleup.backend.entity.converter.CustomExpensesConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "notebook_monthly",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "notebook_year", "notebook_month"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotebookMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "notebook_year", nullable = false)
    private Integer year;

    @Column(name = "notebook_month", nullable = false, length = 20)
    private String month;

    @Column(name = "fixed_maintenance", nullable = false, precision = 38, scale = 2)
    private BigDecimal fixedMaintenance;

    @Column(name = "resident_count", nullable = false)
    private Integer residentCount;

    @Column(name = "water_bill_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal waterBillAmount;

    @Column(name = "use_previous_balance", nullable = false)
    private Boolean usePreviousBalance;

    @Column(name = "opening_balance", nullable = false, precision = 38, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "collection_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal collectionAmount;

    @Column(name = "total_budget", nullable = false, precision = 38, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "total_expenses", nullable = false, precision = 38, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "closing_balance", nullable = false, precision = 38, scale = 2)
    private BigDecimal closingBalance;

    @Convert(converter = CustomExpensesConverter.class)
    @Column(name = "expense_breakdown", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, BigDecimal> expenseBreakdown = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
