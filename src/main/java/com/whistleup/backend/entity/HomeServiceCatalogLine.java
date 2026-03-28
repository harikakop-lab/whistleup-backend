package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "home_service_catalog_line",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subcategory_id", "line_key"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceCatalogLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private HomeServiceSubcategory subcategory;

    @Column(name = "line_key", nullable = false, length = 160)
    private String lineKey;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "variant_label", length = 200)
    private String variantLabel;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "catalogLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Fetch(FetchMode.SUBSELECT)
    private List<HomeServicePricedOption> pricedOptions = new ArrayList<>();
}
