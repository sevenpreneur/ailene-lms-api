package com.ailene.lms.preassessment;

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
import java.util.Map;

@Entity
@Table(name = "lms_pre_assessment_reports")
@Getter
@Setter
public class PreAssessmentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pre_assessment_id", nullable = false, unique = true)
    private Integer preAssessmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, insertable = false)
    private PreAssessmentReportStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> recommendations;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "queued_at", nullable = false, insertable = false)
    private OffsetDateTime queuedAt;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
