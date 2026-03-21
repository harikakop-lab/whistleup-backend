package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "home_service_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_key", nullable = false, length = 80)
    private String categoryKey;

    @Column(name = "category_label", nullable = false, length = 120)
    private String categoryLabel;

    @Column(name = "category_icon", length = 80)
    private String categoryIcon;

    @Column(name = "subcategory_key", nullable = false, length = 120)
    private String subcategoryKey;

    @Column(name = "subcategory_label", nullable = false, length = 180)
    private String subcategoryLabel;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price")
    private Integer price;

    @Column(name = "image", length = 500)
    private String image;

    @Column(name = "popular")
    private Boolean popular;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
