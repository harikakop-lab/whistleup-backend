package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "contact_name", nullable = false)
    private String name;

    @Column(name = "contact_phone", nullable = false)
    private String phone;

    @Column(name = "contact_kind", length = 32)
    private String contactKind;

    /**
     * Many contacts belong to one profile
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "profile_phone",
        referencedColumnName = "phone",
        nullable = false
    )
    private Profile profile;
}
