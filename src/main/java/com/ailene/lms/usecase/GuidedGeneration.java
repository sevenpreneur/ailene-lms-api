package com.ailene.lms.usecase;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_guided_use_case_generations")
@Getter
@Setter
public class GuidedGeneration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(nullable = false)
    private Integer version;

    @Column
    private String feedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String workflow;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
