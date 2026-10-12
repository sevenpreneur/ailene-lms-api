package com.ailene.lms.preassessment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PreAssessmentAttemptRepository extends JpaRepository<PreAssessmentAttempt, Integer> {

    Optional<PreAssessmentAttempt> findByAccessId(String accessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PreAssessmentAttempt a WHERE a.id = :id")
    Optional<PreAssessmentAttempt> findByIdForUpdate(@Param("id") Integer id);

    @Query(value = """
            SELECT f.id AS id,
                   f.code AS code,
                   f.name AS name,
                   f.duration_minutes::int AS durationMinutes,
                   f.status::text AS status
            FROM lms_pa_forms f
            WHERE f.status = 'active'
            """, nativeQuery = true)
    Optional<PreAssessmentFormProjection> findActiveForm();

    @Query(value = """
            SELECT f.id AS id,
                   f.code AS code,
                   f.name AS name,
                   f.duration_minutes::int AS durationMinutes,
                   f.status::text AS status
            FROM lms_pa_forms f
            WHERE f.id = :formId
            """, nativeQuery = true)
    Optional<PreAssessmentFormProjection> findForm(@Param("formId") Integer formId);

    @Query(value = """
            SELECT i.id AS id,
                   i.item_code AS itemCode,
                   i.section::text AS section,
                   i.type::text AS type,
                   i.question AS question,
                   i.domain_id::int AS domainId,
                   i.dimension::text AS dimension,
                   i.cfte_level::int AS cfteLevel,
                   i.is_critical AS isCritical,
                   i.is_behaviour_gate AS isBehaviourGate,
                   i.overclaim_item_id AS overclaimItemId,
                   i.grading_config::text AS gradingConfig
            FROM lms_pa_items i
            WHERE i.form_id = :formId
            ORDER BY i.order_index, i.id
            """, nativeQuery = true)
    List<PreAssessmentItemProjection> findItems(@Param("formId") Integer formId);

    @Query(value = """
            SELECT o.id AS id,
                   o.item_id AS itemId,
                   o.text AS text,
                   o.credit AS credit,
                   o.is_dont_know AS isDontKnow,
                   o.flag AS flag
            FROM lms_pa_item_options o
            JOIN lms_pa_items i ON i.id = o.item_id
            WHERE i.form_id = :formId
            ORDER BY o.item_id, o.order_index, o.id
            """, nativeQuery = true)
    List<PreAssessmentOptionProjection> findOptions(@Param("formId") Integer formId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_pa_answers (attempt_id, item_id, form_id, option_id, text_value, credit)
            VALUES (:attemptId, :itemId, :formId, CAST(:optionId AS INTEGER), CAST(:textValue AS TEXT),
                    CAST(:credit AS NUMERIC))
            """, nativeQuery = true)
    int insertAnswer(@Param("attemptId") Integer attemptId, @Param("itemId") Integer itemId,
            @Param("formId") Integer formId, @Param("optionId") Integer optionId,
            @Param("textValue") String textValue, @Param("credit") BigDecimal credit);

    @Query(value = """
            SELECT a.item_id AS itemId,
                   a.text_value AS textValue,
                   a.credit AS credit,
                   a.rubric_total::int AS rubricTotal,
                   a.has_sensitive_data AS hasSensitiveData,
                   a.ai_feedback AS aiFeedback,
                   (a.graded_at IS NOT NULL) AS isGraded
            FROM lms_pa_answers a
            WHERE a.attempt_id = :attemptId
            """, nativeQuery = true)
    List<PreAssessmentAnswerProjection> findAnswers(@Param("attemptId") Integer attemptId);

    @Modifying
    @Query(value = """
            UPDATE lms_pa_answers
            SET credit = CAST(:credit AS NUMERIC),
                rubric_scores = CAST(:rubricScores AS JSONB),
                rubric_total = :rubricTotal,
                has_sensitive_data = :hasSensitiveData,
                ai_feedback = CAST(:aiFeedback AS TEXT),
                graded_at = CURRENT_TIMESTAMP
            WHERE attempt_id = :attemptId AND item_id = :itemId AND graded_at IS NULL
            """, nativeQuery = true)
    int updateGrade(@Param("attemptId") Integer attemptId, @Param("itemId") Integer itemId,
            @Param("credit") BigDecimal credit, @Param("rubricScores") String rubricScores,
            @Param("rubricTotal") Integer rubricTotal, @Param("hasSensitiveData") boolean hasSensitiveData,
            @Param("aiFeedback") String aiFeedback);

    @Query(value = """
            SELECT c.id AS id,
                   c.security_gate_max_band::int AS securityGateMaxBand,
                   c.behaviour_gate_from_band::int AS behaviourGateFromBand,
                   c.behaviour_gate_max_band::int AS behaviourGateMaxBand,
                   c.behaviour_gate_min_score AS behaviourGateMinScore,
                   c.behaviour_gate_min_credit AS behaviourGateMinCredit,
                   c.overclaim_min_credit AS overclaimMinCredit,
                   c.overclaim_capped_credit AS overclaimCappedCredit,
                   c.target_band::int AS targetBand
            FROM lms_pa_scoring_configs c
            WHERE (CAST(:configId AS INTEGER) IS NULL AND c.is_active) OR c.id = CAST(:configId AS INTEGER)
            """, nativeQuery = true)
    Optional<PreAssessmentConfigProjection> findConfig(@Param("configId") Integer configId);

    @Query(value = """
            SELECT b.band::int AS band,
                   b.name AS name,
                   b.level::int AS level,
                   b.min_p1 AS minP1,
                   b.min_p2 AS minP2,
                   b.min_p3 AS minP3
            FROM lms_pa_bands b
            WHERE b.config_id = :configId
            ORDER BY b.band
            """, nativeQuery = true)
    List<PreAssessmentBandProjection> findBands(@Param("configId") Integer configId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_pa_attempt_domain_scores (attempt_id, domain_id, score, item_count)
            VALUES (:attemptId, :domainId, :score, :itemCount)
            """, nativeQuery = true)
    int insertDomainScore(@Param("attemptId") Integer attemptId, @Param("domainId") Integer domainId,
            @Param("score") BigDecimal score, @Param("itemCount") Integer itemCount);

    @Query(value = """
            SELECT d.code AS domainCode,
                   d.name AS domainName,
                   c.code AS clusterCode,
                   c.name AS clusterName,
                   s.score AS score,
                   s.item_count::int AS itemCount
            FROM lms_pa_attempt_domain_scores s
            JOIN lms_pa_domains d ON d.id = s.domain_id
            JOIN lms_pa_clusters c ON c.id = d.cluster_id
            WHERE s.attempt_id = :attemptId
            ORDER BY c.order_index, d.cfte_number
            """, nativeQuery = true)
    List<PreAssessmentDomainScoreProjection> findDomainScores(@Param("attemptId") Integer attemptId);
}
