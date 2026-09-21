package com.ailene.lms.sponsor;

import com.ailene.lms.access.Access;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SponsorRepository extends JpaRepository<Access, String> {

    @Query(value = """
            SELECT a.id AS accessId,
                   a.role AS role,
                   a.group_id AS groupId,
                   g.name AS groupName,
                   a.current_level_id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   a.created_at AS createdAt,
                   u.id AS userId,
                   u.full_name AS fullName,
                   u.email AS email,
                   u.avatar AS avatar,
                   u.job_title AS jobTitle,
                   u.last_active_at AS lastActiveAt
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            LEFT JOIN lms_levels lv ON lv.id = a.current_level_id
            WHERE a.project_id = :projectId
            ORDER BY a.created_at ASC, a.id ASC
            """, nativeQuery = true)
    List<MemberRowProjection> findMembers(@Param("projectId") String projectId);

    @Query(value = """
            SELECT s.student_access_id AS accessId,
                   a.group_id AS groupId,
                   s.use_case_id AS useCaseId,
                   uc.name AS useCaseName,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.is_accepted AS accepted,
                   s.hours_with_ai AS hoursWithAi,
                   s.hours_without_ai AS hoursWithoutAi
            FROM lms_use_case_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            LEFT JOIN lms_levels lv ON lv.id = uc.level_id
            WHERE a.project_id = :projectId
            """, nativeQuery = true)
    List<UseCaseRowProjection> findUseCaseSubmissions(@Param("projectId") String projectId);

    @Query(value = """
            SELECT s.student_access_id AS accessId,
                   a.group_id AS groupId,
                   s.submitted_at AS submittedAt,
                   s.reviewed_at AS reviewedAt,
                   s.is_accepted AS accepted
            FROM lms_prompt_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            WHERE a.project_id = :projectId
            """, nativeQuery = true)
    List<PromptRowProjection> findPromptSubmissions(@Param("projectId") String projectId);

    @Query(value = """
            SELECT x.student_access_id AS accessId,
                   x.xp_earned AS xpEarned,
                   x.earned_at AS earnedAt
            FROM lms_xp_earnings x
            JOIN lms_accesses a ON a.id = x.student_access_id
            WHERE a.project_id = :projectId
            """, nativeQuery = true)
    List<XpRowProjection> findXpEarnings(@Param("projectId") String projectId);

    @Query(value = """
            SELECT h.access_id AS accessId,
                   lv.level_number AS levelNumber,
                   h.reached_at AS reachedAt
            FROM lms_level_history h
            JOIN lms_accesses a ON a.id = h.access_id
            JOIN lms_levels lv ON lv.id = h.level_id
            WHERE a.project_id = :projectId
            """, nativeQuery = true)
    List<LevelHistoryRowProjection> findLevelHistory(@Param("projectId") String projectId);

    @Query(value = """
            SELECT lv.id AS id, lv.level_number AS levelNumber, lv.name AS name
            FROM lms_levels lv
            WHERE lv.status = 'active'
            ORDER BY lv.level_number ASC
            """, nativeQuery = true)
    List<LevelRowProjection> findActiveLevels();

    @Query(value = """
            SELECT g.id AS id, g.name AS name
            FROM lms_groups g
            WHERE g.project_id = :projectId
            ORDER BY g.name ASC
            """, nativeQuery = true)
    List<GroupRowProjection> findGroups(@Param("projectId") String projectId);

    @Query(value = """
            SELECT a.id AS accessId, u.full_name AS fullName, u.avatar AS avatar, u.job_title AS jobTitle
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            WHERE a.project_id = :projectId AND a.group_id = :groupId AND a.role = 'champion'
            ORDER BY a.created_at ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ChampionRowProjection> findGroupChampion(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId);

    @Query(value = """
            SELECT (
              (SELECT COUNT(*) FROM lms_materials m
                 JOIN lms_chapters c ON c.id = m.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE c.project_id = :projectId AND c.status = 'active' AND m.status = 'active') +
              (SELECT COUNT(*) FROM lms_videos v
                 JOIN lms_chapters c ON c.id = v.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE c.project_id = :projectId AND c.status = 'active' AND v.status = 'active') +
              (SELECT COUNT(*) FROM lms_quizzes q
                 JOIN lms_chapters c ON c.id = q.chapter_id
                 JOIN lms_levels lv ON lv.id = c.level_id
                 WHERE c.project_id = :projectId AND c.status = 'active' AND q.status = 'active')
            )
            """, nativeQuery = true)
    long countLearningTasks(@Param("projectId") String projectId);

    @Query(value = """
            SELECT t.access_id AS accessId, SUM(t.done_count) AS doneCount FROM (
              SELECT mc.student_access_id AS access_id, COUNT(*) AS done_count
              FROM lms_material_completions mc
              JOIN lms_accesses a ON a.id = mc.student_access_id
              WHERE a.project_id = :projectId
              GROUP BY mc.student_access_id
              UNION ALL
              SELECT vc.student_access_id AS access_id, COUNT(*) AS done_count
              FROM lms_video_completions vc
              JOIN lms_accesses a ON a.id = vc.student_access_id
              WHERE a.project_id = :projectId
              GROUP BY vc.student_access_id
              UNION ALL
              SELECT qs.student_access_id AS access_id, COUNT(DISTINCT qs.quiz_id) AS done_count
              FROM lms_quiz_submissions qs
              JOIN lms_accesses a ON a.id = qs.student_access_id
              WHERE a.project_id = :projectId AND qs.is_completed = true
              GROUP BY qs.student_access_id
            ) t
            GROUP BY t.access_id
            """, nativeQuery = true)
    List<TaskDoneProjection> findTasksDone(@Param("projectId") String projectId);

    @Query(value = """
            SELECT s.submitted_at AS at,
                   u.full_name AS actor,
                   g.name AS groupName,
                   uc.name AS subject,
                   s.is_accepted AS accepted
            FROM lms_use_case_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            WHERE a.project_id = :projectId AND s.submitted_at IS NOT NULL
            ORDER BY s.submitted_at DESC
            LIMIT :take
            """, nativeQuery = true)
    List<ActivityRowProjection> findRecentUseCaseSubmissions(@Param("projectId") String projectId,
            @Param("take") int take);

    @Query(value = """
            SELECT s.reviewed_at AS at,
                   reviewer.full_name AS actor,
                   g.name AS groupName,
                   uc.name AS subject,
                   s.is_accepted AS accepted
            FROM lms_use_case_submissions s
            JOIN lms_accesses a ON a.id = s.student_access_id
            JOIN lms_use_cases uc ON uc.id = s.use_case_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            LEFT JOIN lms_accesses ra ON ra.id = s.reviewed_by_access_id
            LEFT JOIN lms_users reviewer ON reviewer.id = ra.user_id
            WHERE a.project_id = :projectId AND s.reviewed_at IS NOT NULL
            ORDER BY s.reviewed_at DESC
            LIMIT :take
            """, nativeQuery = true)
    List<ActivityRowProjection> findRecentUseCaseReviews(@Param("projectId") String projectId,
            @Param("take") int take);

    @Query(value = """
            SELECT pa.created_at AS at,
                   u.full_name AS actor,
                   g.name AS groupName,
                   u.job_title AS subject,
                   FALSE AS accepted
            FROM lms_pre_assessments pa
            JOIN lms_accesses a ON a.id = pa.access_id
            JOIN lms_users u ON u.id = a.user_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            WHERE a.project_id = :projectId
            ORDER BY pa.created_at DESC
            LIMIT :take
            """, nativeQuery = true)
    List<ActivityRowProjection> findRecentPreAssessments(@Param("projectId") String projectId,
            @Param("take") int take);

    @Query(value = """
            SELECT pa.id AS preAssessmentId, a.group_id AS groupId
            FROM lms_pre_assessments pa
            JOIN lms_accesses a ON a.id = pa.access_id
            WHERE a.project_id = :projectId AND a.group_id IS NOT NULL
            """, nativeQuery = true)
    List<PreAssessmentGroupProjection> findPreAssessmentGroups(@Param("projectId") String projectId);
}
