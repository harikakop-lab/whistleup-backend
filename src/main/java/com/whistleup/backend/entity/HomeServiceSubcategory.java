package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "home_service_subcategory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "subcategory_key"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private HomeServiceCategory category;

    @Column(name = "subcategory_key", nullable = false, length = 120)
    private String subcategoryKey;

    @Column(name = "subcategory_label", nullable = false, length = 180)
    private String subcategoryLabel;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image", length = 500)
    private String image;

    @Column(name = "popular")
    private Boolean popular;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "subcategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Fetch(FetchMode.SUBSELECT)
    private List<HomeServiceCatalogLine> catalogLines = new ArrayList<>();
}
