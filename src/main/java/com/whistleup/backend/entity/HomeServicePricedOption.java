package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "home_service_priced_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"catalog_line_id", "option_key"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServicePricedOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_line_id", nullable = false)
    private HomeServiceCatalogLine catalogLine;

    @Column(name = "option_key", nullable = false, length = 160)
    private String optionKey;

    @Column(name = "option_label", nullable = false, length = 200)
    private String optionLabel;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
