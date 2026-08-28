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

@Entity
@Table(name = "lms_pre_assessments")
@Getter
@Setter
public class PreAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "access_id", nullable = false, unique = true, length = 21)
    private String accessId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ai_use_frequency", nullable = false)
    private PreAssessmentAiUseFrequency aiUseFrequency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ai_tools_used", columnDefinition = "text[]")
    private String[] aiToolsUsed;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ai_limitations", columnDefinition = "text[]")
    private String[] aiLimitations;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "output_review", nullable = false)
    private PreAssessmentOutputReview outputReview;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "use_cases", columnDefinition = "text[]")
    private String[] useCases;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "team_adoption", nullable = false)
    private PreAssessmentTeamAdoption teamAdoption;

    @Column(name = "concrete_example")
    private String concreteExample;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "model_selection", nullable = false)
    private PreAssessmentFrequency modelSelection;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "multimodal_use", nullable = false)
    private PreAssessmentFrequency multimodalUse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "workflow_reuse", nullable = false)
    private PreAssessmentFrequency workflowReuse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "prompt_comfort", nullable = false)
    private PreAssessmentPromptSkill promptComfort;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "prompt_iteration", nullable = false)
    private PreAssessmentFrequency promptIteration;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "refine_scenario", nullable = false)
    private PreAssessmentRefineScenario refineScenario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "professional_attitude", nullable = false)
    private PreAssessmentAttitude professionalAttitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "data_safety_check", nullable = false)
    private PreAssessmentFrequency dataSafetyCheck;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "publish_unchecked", nullable = false)
    private PreAssessmentFrequency publishUnchecked;

    @Column(name = "biggest_challenge", nullable = false)
    private String biggestChallenge;

    @Column(name = "training_expectation", nullable = false)
    private String trainingExpectation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PreAssessmentMotivation motivation;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
