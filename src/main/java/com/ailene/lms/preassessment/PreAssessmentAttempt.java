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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_pa_attempts")
@Getter
@Setter
public class PreAssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "access_id", nullable = false, unique = true, length = 21)
    private String accessId;

    @Column(name = "form_id", nullable = false)
    private Integer formId;

    @Column(name = "config_id")
    private Integer configId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PreAssessmentAttemptStatus status;

    @Column(precision = 5, scale = 2)
    private BigDecimal p1;

    @Column(precision = 5, scale = 2)
    private BigDecimal p2;

    @Column(precision = 5, scale = 2)
    private BigDecimal p3;

    @Column(name = "knowledge_score", precision = 5, scale = 2)
    private BigDecimal knowledgeScore;

    @Column(name = "skills_score", precision = 5, scale = 2)
    private BigDecimal skillsScore;

    @Column(name = "behaviours_score", precision = 5, scale = 2)
    private BigDecimal behavioursScore;

    @Column(name = "raw_band")
    private Short rawBand;

    @Column(name = "final_band")
    private Short finalBand;

    @Column(name = "level")
    private Short level;

    @Column(name = "security_gate_passed")
    private Boolean securityGatePassed;

    @Column(name = "behaviour_gate_passed")
    private Boolean behaviourGatePassed;

    @Column(name = "is_overclaim")
    private Boolean isOverclaim;

    @Column(name = "is_shadow_ai")
    private Boolean isShadowAi;

    @Column(name = "started_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "scored_at")
    private OffsetDateTime scoredAt;
}
