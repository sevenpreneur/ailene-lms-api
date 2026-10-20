package com.ailene.lms.usecase;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_guided_use_case_sessions")
@Getter
@Setter
public class GuidedSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "project_id", nullable = false, length = 21)
    private String projectId;

    @Column(name = "use_case_id")
    private Integer useCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GuidedStatus status = GuidedStatus.intake;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep = 1;

    @Column(name = "daily_task")
    private String dailyTask;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column
    private UseCaseFrequency frequency;

    @Column(name = "example_text")
    private String exampleText;

    @Column(name = "example_file_url", length = 500)
    private String exampleFileUrl;

    @Column(name = "sensitive_data_confirmed", nullable = false)
    private boolean sensitiveDataConfirmed;

    @Column(name = "minutes_without_ai", precision = 8, scale = 2)
    private BigDecimal minutesWithoutAi;

    @Column(name = "current_process")
    private String currentProcess;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_tools", nullable = false)
    private String currentToolsJson = "[]";

    @Column(name = "pain_point")
    private String painPoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", nullable = false)
    private String stepsJson = "[]";

    @Column
    private String summary;

    @Column(name = "estimated_minutes_with_ai")
    private Integer estimatedMinutesWithAi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risks", nullable = false)
    private String risksJson = "[]";

    @Column(name = "generations_used", nullable = false)
    private Integer generationsUsed = 0;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "generation_token", length = 21)
    private String generationToken;

    @Column(name = "generation_started_at")
    private OffsetDateTime generationStartedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
