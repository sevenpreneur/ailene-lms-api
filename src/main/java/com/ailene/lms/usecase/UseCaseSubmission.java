package com.ailene.lms.usecase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_use_case_submissions")
@Getter
@Setter
public class UseCaseSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @Column(name = "use_case_id", nullable = false)
    private Integer useCaseId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "assigned_by_access_id", length = 21)
    private String assignedByAccessId;

    @Column
    private OffsetDateTime deadline;

    @Column
    private String message;

    @Column(name = "outcome_proof")
    private String outcomeProof;

    @Column(name = "hours_with_ai", precision = 6, scale = 2)
    private BigDecimal hoursWithAi;

    @Column(name = "hours_without_ai", precision = 6, scale = 2)
    private BigDecimal hoursWithoutAi;

    @Column
    private String description;

    @Column(name = "ai_tool")
    private String aiTool;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column
    private UseCaseFrequency frequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column
    private UseCaseType type;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reviewed_by_access_id", length = 21)
    private String reviewedByAccessId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column
    private String comment;

    @Column(name = "is_accepted", nullable = false, insertable = false)
    private Boolean isAccepted;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
