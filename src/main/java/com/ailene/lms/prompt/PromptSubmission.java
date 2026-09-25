package com.ailene.lms.prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_prompt_submissions")
@Getter
@Setter
public class PromptSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @Column(name = "prompt_id", nullable = false)
    private Integer promptId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "assigned_by_access_id", length = 21)
    private String assignedByAccessId;

    @Column
    private OffsetDateTime deadline;

    @Column
    private String message;

    @Column
    private String input;

    @Column
    private String output;

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

    @Column(name = "rubric_specificity")
    private Short rubricSpecificity;

    @Column(name = "rubric_context")
    private Short rubricContext;

    @Column(name = "rubric_constraints")
    private Short rubricConstraints;

    @Column(name = "rubric_examples")
    private Short rubricExamples;

    @Column(name = "rubric_iteration")
    private Short rubricIteration;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", length = 10)
    private PromptEvaluationStatus aiStatus;

    @Column(name = "ai_feedback")
    private String aiFeedback;

    @Column(name = "ai_evaluated_at")
    private OffsetDateTime aiEvaluatedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
