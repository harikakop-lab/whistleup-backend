package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "home_service_category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_key", nullable = false, unique = true, length = 80)
    private String categoryKey;

    @Column(name = "category_label", nullable = false, length = 120)
    private String categoryLabel;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "category_icon", length = 80)
    private String categoryIcon;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Fetch(FetchMode.SUBSELECT)
    private List<HomeServiceSubcategory> subcategories = new ArrayList<>();
}
