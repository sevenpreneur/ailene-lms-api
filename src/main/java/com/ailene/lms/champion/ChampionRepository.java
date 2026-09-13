package com.ailene.lms.champion;

import com.ailene.lms.access.Access;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChampionRepository extends JpaRepository<Access, String> {

    @Query(value = """
            SELECT a.id AS accessId,
                   a.group_id AS groupId,
                   g.name AS groupName,
                   a.current_level_id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   a.created_at AS joinedAt,
                   u.id AS userId,
                   u.full_name AS fullName,
                   u.email AS email,
                   u.avatar AS avatar,
                   u.job_title AS jobTitle,
                   u.last_active_at AS lastActiveAt
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            LEFT JOIN lms_levels lv ON lv.id = a.current_level_id AND lv.project_id = a.project_id
            WHERE a.project_id = :projectId AND a.group_id = :groupId AND a.id <> :championAccessId
            ORDER BY u.full_name ASC
            """, nativeQuery = true)
    List<TeamMemberProjection> findTeamMembers(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId, @Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT COUNT(*) FROM lms_accesses a
            WHERE a.project_id = :projectId AND a.id IN (:accessIds) AND a.group_id = :groupId
            """, nativeQuery = true)
    long countAccessesInGroup(@Param("projectId") String projectId, @Param("accessIds") List<String> accessIds,
            @Param("groupId") Integer groupId);

    @Query(value = """
            SELECT (
              (SELECT COUNT(*) FROM lms_materials m
                 JOIN lms_chapters c ON c.id = m.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE lv.project_id = :projectId AND c.status = 'active' AND m.status = 'active') +
              (SELECT COUNT(*) FROM lms_videos v
                 JOIN lms_chapters c ON c.id = v.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE lv.project_id = :projectId AND c.status = 'active' AND v.status = 'active') +
              (SELECT COUNT(*) FROM lms_quizzes q
                 JOIN lms_chapters c ON c.id = q.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE lv.project_id = :projectId AND c.status = 'active' AND q.status = 'active')
            )
            """, nativeQuery = true)
    long countLearningTasks(@Param("projectId") String projectId);

    @Query(value = """
            SELECT t.access_id AS accessId, SUM(t.done_count) AS doneCount FROM (
              SELECT mc.student_access_id AS access_id, COUNT(*) AS done_count
              FROM lms_material_completions mc
              WHERE mc.student_access_id IN (:accessIds)
              GROUP BY mc.student_access_id
              UNION ALL
              SELECT vc.student_access_id AS access_id, COUNT(*) AS done_count
              FROM lms_video_completions vc
              WHERE vc.student_access_id IN (:accessIds)
              GROUP BY vc.student_access_id
              UNION ALL
              SELECT qs.student_access_id AS access_id, COUNT(DISTINCT qs.quiz_id) AS done_count
              FROM lms_quiz_submissions qs
              WHERE qs.student_access_id IN (:accessIds) AND qs.is_completed = true
              GROUP BY qs.student_access_id
            ) t
            GROUP BY t.access_id
            """, nativeQuery = true)
    List<AccessCountProjection> findTasksDone(@Param("accessIds") List<String> accessIds);

    @Query(value = """
            SELECT xe.student_access_id AS accessId, COALESCE(SUM(xe.xp_earned), 0) AS total
            FROM lms_xp_earnings xe
            WHERE xe.student_access_id IN (:accessIds)
            GROUP BY xe.student_access_id
            """, nativeQuery = true)
    List<AccessTotalProjection> findXpTotals(@Param("accessIds") List<String> accessIds);

    @Query(value = """
            SELECT s.student_access_id AS accessId,
                   s.submitted_at AS submittedAt,
                   s.is_accepted AS accepted,
                   s.hours_with_ai AS hoursWithAi,
                   s.hours_without_ai AS hoursWithoutAi,
                   s.ai_tool AS aiTool
            FROM lms_use_case_submissions s
            WHERE s.student_access_id IN (:accessIds)
            """, nativeQuery = true)
    List<TeamUseCaseProjection> findTeamUseCaseSubmissions(@Param("accessIds") List<String> accessIds);

    @Query(value = """
            SELECT s.id AS id,
                   s.student_access_id AS accessId,
                   u.full_name AS fullName,
                   u.avatar AS avatar,
                   p.id AS itemId,
                   p.name AS itemName,
                   p.scenario AS itemText,
                   lv.id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   s.deadline AS deadline,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.is_accepted AS accepted,
                   CAST(NULL AS NUMERIC) AS hoursWithAi,
                   CAST(NULL AS VARCHAR) AS aiTool
            FROM lms_prompt_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_prompts p ON p.id = s.prompt_id
            LEFT JOIN lms_levels lv ON lv.id = p.level_id
            WHERE s.assigned_by_access_id = :championAccessId
            ORDER BY s.is_accepted ASC, s.submitted_at DESC NULLS LAST
            """, nativeQuery = true)
    List<ReviewQueueProjection> findPromptReviewQueue(@Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT s.id AS id,
                   s.student_access_id AS accessId,
                   u.full_name AS fullName,
                   u.avatar AS avatar,
                   uc.id AS itemId,
                   uc.name AS itemName,
                   uc.description AS itemText,
                   lv.id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   s.deadline AS deadline,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.is_accepted AS accepted,
                   s.hours_with_ai AS hoursWithAi,
                   s.ai_tool AS aiTool
            FROM lms_use_case_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            LEFT JOIN lms_levels lv ON lv.id = uc.level_id
            WHERE s.assigned_by_access_id = :championAccessId
            ORDER BY s.is_accepted ASC, s.submitted_at DESC NULLS LAST
            """, nativeQuery = true)
    List<ReviewQueueProjection> findUseCaseReviewQueue(@Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT pc.prompt_id AS ownerId, c.id AS categoryId, c.name AS categoryName
            FROM lms_prompt_categories pc
            JOIN lms_categories c ON c.id = pc.category_id
            WHERE pc.prompt_id IN (:promptIds)
            """, nativeQuery = true)
    List<ItemCategoryProjection> findPromptCategories(@Param("promptIds") List<Integer> promptIds);

    @Query(value = """
            SELECT ucc.use_case_id AS ownerId, c.id AS categoryId, c.name AS categoryName
            FROM lms_use_case_categories ucc
            JOIN lms_categories c ON c.id = ucc.category_id
            WHERE ucc.use_case_id IN (:useCaseIds)
            """, nativeQuery = true)
    List<ItemCategoryProjection> findUseCaseCategories(@Param("useCaseIds") List<Integer> useCaseIds);

    @Query(value = """
            SELECT s.id AS id,
                   s.student_access_id AS accessId,
                   u.full_name AS fullName,
                   u.email AS email,
                   u.avatar AS avatar,
                   p.id AS itemId,
                   p.name AS itemName,
                   p.scenario AS itemText,
                   p.expected_output AS expectedOutput,
                   lv.id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   reviewer.full_name AS reviewerName,
                   reviewer.avatar AS reviewerAvatar,
                   s.reviewed_by_access_id AS reviewerAccessId,
                   s.deadline AS deadline,
                   s.message AS message,
                   s.input AS input,
                   s.output AS output,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.comment AS comment,
                   s.is_accepted AS accepted,
                   s.rubric_specificity AS rubricSpecificity,
                   s.rubric_context AS rubricContext,
                   s.rubric_constraints AS rubricConstraints,
                   s.rubric_examples AS rubricExamples,
                   s.rubric_iteration AS rubricIteration
            FROM lms_prompt_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_prompts p ON p.id = s.prompt_id
            LEFT JOIN lms_levels lv ON lv.id = p.level_id
            LEFT JOIN lms_accesses ra ON ra.id = s.reviewed_by_access_id
            LEFT JOIN lms_users reviewer ON reviewer.id = ra.user_id
            WHERE s.id = :submissionId AND s.assigned_by_access_id = :championAccessId
            """, nativeQuery = true)
    List<PromptSubmissionDetailProjection> findPromptSubmissionDetail(@Param("submissionId") Integer submissionId,
            @Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT s.id AS id,
                   s.student_access_id AS accessId,
                   u.full_name AS fullName,
                   u.email AS email,
                   u.avatar AS avatar,
                   uc.id AS itemId,
                   uc.name AS itemName,
                   uc.description AS itemText,
                   lv.id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   reviewer.full_name AS reviewerName,
                   reviewer.avatar AS reviewerAvatar,
                   s.reviewed_by_access_id AS reviewerAccessId,
                   s.deadline AS deadline,
                   s.message AS message,
                   s.outcome_proof AS outcomeProof,
                   s.hours_with_ai AS hoursWithAi,
                   s.hours_without_ai AS hoursWithoutAi,
                   s.description AS description,
                   s.ai_tool AS aiTool,
                   s.frequency AS frequency,
                   s.type AS type,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.comment AS comment,
                   s.is_accepted AS accepted
            FROM lms_use_case_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            LEFT JOIN lms_levels lv ON lv.id = uc.level_id
            LEFT JOIN lms_accesses ra ON ra.id = s.reviewed_by_access_id
            LEFT JOIN lms_users reviewer ON reviewer.id = ra.user_id
            WHERE s.id = :submissionId AND s.assigned_by_access_id = :championAccessId
            """, nativeQuery = true)
    List<UseCaseSubmissionDetailProjection> findUseCaseSubmissionDetail(
            @Param("submissionId") Integer submissionId, @Param("championAccessId") String championAccessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_prompt_submissions (student_access_id, prompt_id, assigned_by_access_id, deadline, message)
            VALUES (:accessId, :promptId, :championAccessId, :deadline, :message)
            ON CONFLICT (student_access_id, prompt_id) DO NOTHING
            """, nativeQuery = true)
    int insertPromptAssignment(@Param("accessId") String accessId, @Param("promptId") Integer promptId,
            @Param("championAccessId") String championAccessId,
            @Param("deadline") java.time.OffsetDateTime deadline, @Param("message") String message);

    @Modifying
    @Query(value = """
            INSERT INTO lms_use_case_submissions (student_access_id, use_case_id, assigned_by_access_id, deadline, message)
            VALUES (:accessId, :useCaseId, :championAccessId, :deadline, :message)
            ON CONFLICT (student_access_id, use_case_id) DO NOTHING
            """, nativeQuery = true)
    int insertUseCaseAssignment(@Param("accessId") String accessId, @Param("useCaseId") Integer useCaseId,
            @Param("championAccessId") String championAccessId,
            @Param("deadline") java.time.OffsetDateTime deadline, @Param("message") String message);

    @Modifying
    @Query(value = """
            INSERT INTO lms_xp_earnings (student_access_id, learning_type, learning_id, xp_earned)
            VALUES (:accessId, CAST(:learningType AS lms_learning_type_enum), :learningId, :xpEarned)
            ON CONFLICT (student_access_id, learning_type, learning_id) DO NOTHING
            """, nativeQuery = true)
    int insertXpEarning(@Param("accessId") String accessId, @Param("learningType") String learningType,
            @Param("learningId") String learningId, @Param("xpEarned") Short xpEarned);

    @Query(value = """
            SELECT pa.id AS preAssessmentId,
                   a.id AS accessId,
                   a.group_id AS groupId,
                   u.full_name AS fullName,
                   u.avatar AS avatar
            FROM lms_pre_assessments pa
            JOIN lms_accesses a ON a.id = pa.access_id
            JOIN lms_users u ON u.id = a.user_id
            WHERE a.project_id = :projectId AND a.group_id = :groupId
            """, nativeQuery = true)
    List<TeamPreAssessmentProjection> findTeamPreAssessments(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId);

    @Query(value = """
            SELECT h.access_id AS accessId, lv.level_number AS levelNumber, h.reached_at AS reachedAt
            FROM lms_level_history h
            JOIN lms_levels lv ON lv.id = h.level_id
            WHERE h.access_id IN (:accessIds)
            """, nativeQuery = true)
    List<TeamLevelHistoryProjection> findTeamLevelHistory(@Param("accessIds") List<String> accessIds);

    @Query(value = """
            SELECT a.id AS accessId, u.full_name AS fullName, u.avatar AS avatar, u.job_title AS jobTitle
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            WHERE a.project_id = :projectId AND a.role = 'sponsor'
            ORDER BY a.created_at ASC
            LIMIT 1
            """, nativeQuery = true)
    List<SponsorRecipientProjection> findProjectSponsor(@Param("projectId") String projectId);

    @Query(value = """
            SELECT qs.id AS id, qs.quiz_id AS quizId, q.name AS quizName, qs.score AS score,
                   qs.submitted_at AS submittedAt
            FROM lms_quiz_submissions qs
            JOIN lms_quizzes q ON q.id = qs.quiz_id
            WHERE qs.student_access_id = :accessId AND qs.is_completed = true
            ORDER BY qs.submitted_at DESC
            """, nativeQuery = true)
    List<MemberQuizProjection> findMemberQuizSubmissions(@Param("accessId") String accessId);

    @Query(value = """
            SELECT s.id AS id, p.name AS itemName, CAST(NULL AS VARCHAR) AS aiTool, s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt, s.is_accepted AS accepted, s.created_at AS createdAt
            FROM lms_prompt_submissions s
            JOIN lms_prompts p ON p.id = s.prompt_id
            WHERE s.student_access_id = :accessId AND s.assigned_by_access_id = :championAccessId
            ORDER BY s.submitted_at DESC NULLS LAST, s.created_at DESC
            """, nativeQuery = true)
    List<MemberSubmissionProjection> findMemberPromptSubmissions(@Param("accessId") String accessId,
            @Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT s.id AS id, uc.name AS itemName, s.ai_tool AS aiTool, s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt, s.is_accepted AS accepted, s.created_at AS createdAt
            FROM lms_use_case_submissions s
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            WHERE s.student_access_id = :accessId AND s.assigned_by_access_id = :championAccessId
            ORDER BY s.submitted_at DESC NULLS LAST, s.created_at DESC
            """, nativeQuery = true)
    List<MemberSubmissionProjection> findMemberUseCaseSubmissions(@Param("accessId") String accessId,
            @Param("championAccessId") String championAccessId);

    @Query(value = """
            SELECT qs.submitted_at AS at FROM lms_quiz_submissions qs
            WHERE qs.student_access_id = :accessId AND qs.is_completed = true
            UNION ALL
            SELECT vc.completed_at FROM lms_video_completions vc WHERE vc.student_access_id = :accessId
            UNION ALL
            SELECT mc.completed_at FROM lms_material_completions mc WHERE mc.student_access_id = :accessId
            """, nativeQuery = true)
    List<ActivityDayProjection> findMemberActivityDays(@Param("accessId") String accessId);

    @Query(value = """
            SELECT (
                     (SELECT COUNT(*) FROM lms_quizzes q
                        JOIN lms_chapters c ON c.id = q.chapter_id
                        WHERE c.level_id = a.current_level_id AND c.status = 'active' AND q.status = 'active') +
                     (SELECT COUNT(*) FROM lms_materials m
                        JOIN lms_chapters c ON c.id = m.chapter_id
                        WHERE c.level_id = a.current_level_id AND c.status = 'active' AND m.status = 'active')
                   ) AS required,
                   (
                     (SELECT COUNT(DISTINCT qs.quiz_id) FROM lms_quiz_submissions qs
                        JOIN lms_quizzes q ON q.id = qs.quiz_id
                        JOIN lms_chapters c ON c.id = q.chapter_id
                        WHERE c.level_id = a.current_level_id AND c.status = 'active' AND q.status = 'active'
                          AND qs.student_access_id = a.id AND qs.is_completed = true) +
                     (SELECT COUNT(*) FROM lms_material_completions mc
                        JOIN lms_materials m ON m.id = mc.material_id
                        JOIN lms_chapters c ON c.id = m.chapter_id
                        WHERE c.level_id = a.current_level_id AND c.status = 'active' AND m.status = 'active'
                          AND mc.student_access_id = a.id)
                   ) AS done,
                   next_level.id AS nextLevelId,
                   next_level.level_number AS nextLevelNumber
            FROM lms_accesses a
            LEFT JOIN lms_levels cur ON cur.id = a.current_level_id
            LEFT JOIN lms_levels next_level ON next_level.project_id = a.project_id
                 AND next_level.status = 'active'
                 AND next_level.level_number = COALESCE(cur.level_number, 0) + 1
            WHERE a.id = :accessId
            """, nativeQuery = true)
    List<GateProgressProjection> findMemberGateProgress(@Param("accessId") String accessId);

    @Query(value = """
            SELECT cn.id AS id, cn.text AS text, cn.created_at AS createdAt, cu.full_name AS championName
            FROM lms_coaching_notes cn
            JOIN lms_accesses ca ON ca.id = cn.champion_access_id
            JOIN lms_users cu ON cu.id = ca.user_id
            WHERE cn.student_access_id = :accessId
            ORDER BY cn.created_at DESC
            LIMIT 20
            """, nativeQuery = true)
    List<MemberNoteProjection> findMemberNotes(@Param("accessId") String accessId);
}
