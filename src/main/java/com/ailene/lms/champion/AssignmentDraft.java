package com.ailene.lms.champion;

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

import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_assignment_drafts")
@Getter
@Setter
public class AssignmentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "batch_id", nullable = false, length = 21)
    private String batchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "champion_access_id", nullable = false, length = 21)
    private String championAccessId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "project_id", nullable = false, length = 21)
    private String projectId;

    @Column(nullable = false)
    private String instruction;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_kind", length = 10)
    private AssignmentKind requestedKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AssignmentKind kind;

    @Column(nullable = false, length = 100)
    private String angle;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "expected_output")
    private String expectedOutput;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "category_ids", nullable = false, columnDefinition = "smallint[]")
    private Short[] categoryIds;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "used_prompt_id")
    private Integer usedPromptId;

    @Column(name = "used_use_case_id")
    private Integer usedUseCaseId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
