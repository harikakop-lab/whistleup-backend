package com.whistleup.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long pollId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "options", nullable = false)
    @JdbcTypeCode(value = SqlTypes.JSON)
    private List<String> options;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "votes")
    @JdbcTypeCode(value = SqlTypes.JSON)
    private Map<String, Integer> votesPerOption;

    @Column(name = "total_votes")
    private Integer totalVotes;

    @Column(name = "created_date")
    private ZonedDateTime timestamp;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed;
}
