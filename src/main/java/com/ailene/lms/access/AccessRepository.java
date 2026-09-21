package com.ailene.lms.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessRepository extends JpaRepository<Access, String> {

    Optional<Access> findByUserIdAndProjectId(UUID userId, String projectId);

    @Query(value = """
            SELECT p.id AS id,
                   p.name AS name,
                   p.company_name AS companyName,
                   p.company_slug AS companySlug,
                   p.company_image_url AS avatar,
                   g.id AS groupId,
                   g.name AS groupName,
                   a.role AS role,
                   EXISTS (SELECT 1 FROM lms_pre_assessments pa WHERE pa.access_id = a.id) AS hasPreAssessment
            FROM lms_accesses a
            JOIN lms_projects p ON p.id = a.project_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            WHERE a.user_id = :userId
            """, nativeQuery = true)
    List<ProjectAccessProjection> findProjectAccessByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT (SELECT COALESCE(SUM(xp_earned), 0) FROM lms_xp_earnings WHERE student_access_id = a.id) AS xpCount,
                   lv.level_number AS currentLevelNumber,
                   EXISTS (SELECT 1 FROM lms_pre_assessments pa WHERE pa.access_id = a.id) AS hasPreAssessment
            FROM lms_accesses a
            LEFT JOIN lms_levels lv ON lv.id = a.current_level_id
            WHERE a.user_id = :userId AND a.project_id = :projectId
            """, nativeQuery = true)
    Optional<StudentStatusProjection> findStudentStatus(@Param("userId") UUID userId,
            @Param("projectId") String projectId);

    @Query(value = """
            SELECT a.id AS accessId,
                   (SELECT COALESCE(SUM(xp_earned), 0) FROM lms_xp_earnings WHERE student_access_id = a.id) AS xpCount,
                   lv.id AS currentLevelId,
                   lv.level_number AS currentLevelNumber,
                   lv.name AS currentLevelName,
                   (
                     (SELECT COUNT(*) FROM lms_quizzes q
                        JOIN lms_chapters c ON c.id = q.chapter_id
                        WHERE c.level_id = lv.id AND c.status = 'active' AND EXISTS (SELECT 1 FROM lms_chapter_sessions s
                                      WHERE s.chapter_id = c.id AND s.status = 'active'
                                        AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)) AND q.status = 'active') +
                     (SELECT COUNT(*) FROM lms_materials m
                        JOIN lms_chapters c ON c.id = m.chapter_id
                        WHERE c.level_id = lv.id AND c.status = 'active' AND EXISTS (SELECT 1 FROM lms_chapter_sessions s
                                      WHERE s.chapter_id = c.id AND s.status = 'active'
                                        AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)) AND m.status = 'active')
                   ) AS tasksRequired,
                   (
                     (SELECT COUNT(DISTINCT qs.quiz_id) FROM lms_quiz_submissions qs
                        JOIN lms_quizzes q ON q.id = qs.quiz_id
                        JOIN lms_chapters c ON c.id = q.chapter_id
                        WHERE c.level_id = lv.id AND c.status = 'active' AND EXISTS (SELECT 1 FROM lms_chapter_sessions s
                                      WHERE s.chapter_id = c.id AND s.status = 'active'
                                        AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)) AND q.status = 'active'
                          AND qs.student_access_id = a.id AND qs.is_completed = true) +
                     (SELECT COUNT(*) FROM lms_material_completions mc
                        JOIN lms_materials m ON m.id = mc.material_id
                        JOIN lms_chapters c ON c.id = m.chapter_id
                        WHERE c.level_id = lv.id AND c.status = 'active' AND EXISTS (SELECT 1 FROM lms_chapter_sessions s
                                      WHERE s.chapter_id = c.id AND s.status = 'active'
                                        AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)) AND m.status = 'active'
                          AND mc.student_access_id = a.id)
                   ) AS tasksDone
            FROM lms_accesses a
            LEFT JOIN lms_levels lv ON lv.id = a.current_level_id
            WHERE a.user_id = :userId AND a.project_id = :projectId
            """, nativeQuery = true)
    Optional<LevelProgressProjection> findLevelProgress(@Param("userId") UUID userId,
            @Param("projectId") String projectId);

    @Query(value = """
            SELECT COUNT(DISTINCT day) FROM (
              SELECT date_trunc('day', qs.submitted_at) AS day
              FROM lms_quiz_submissions qs
              JOIN lms_quizzes q ON q.id = qs.quiz_id
              JOIN lms_chapters c ON c.id = q.chapter_id
              JOIN lms_levels lv ON lv.id = c.level_id
              WHERE c.project_id = :projectId AND qs.student_access_id = :accessId
                AND qs.is_completed = true AND qs.submitted_at >= :since
              UNION ALL
              SELECT date_trunc('day', vc.completed_at)
              FROM lms_video_completions vc
              JOIN lms_videos v ON v.id = vc.video_id
              JOIN lms_chapters c ON c.id = v.chapter_id
              JOIN lms_levels lv ON lv.id = c.level_id
              WHERE c.project_id = :projectId AND vc.student_access_id = :accessId AND vc.completed_at >= :since
              UNION ALL
              SELECT date_trunc('day', mc.completed_at)
              FROM lms_material_completions mc
              JOIN lms_materials m ON m.id = mc.material_id
              JOIN lms_chapters c ON c.id = m.chapter_id
              JOIN lms_levels lv ON lv.id = c.level_id
              WHERE c.project_id = :projectId AND mc.student_access_id = :accessId AND mc.completed_at >= :since
            ) t
            """, nativeQuery = true)
    long countActiveDays(@Param("projectId") String projectId, @Param("accessId") String accessId,
            @Param("since") Instant since);

    @Query(value = """
            SELECT a.id AS accessId, a.group_id AS groupId, g.name AS groupName
            FROM lms_accesses a
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            WHERE a.user_id = :userId AND a.project_id = :projectId
            """, nativeQuery = true)
    Optional<GroupSummaryProjection> findGroupSummary(@Param("userId") UUID userId,
            @Param("projectId") String projectId);

    @Query(value = """
            SELECT a.id AS accessId, u.full_name AS fullName, u.avatar AS avatar,
                   COALESCE((SELECT SUM(xe.xp_earned) FROM lms_xp_earnings xe WHERE xe.student_access_id = a.id), 0) AS totalXp
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            WHERE a.project_id = :projectId AND a.group_id = :groupId
            ORDER BY totalXp DESC
            """, nativeQuery = true)
    List<LeaderboardRowProjection> findGroupLeaderboard(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId);

    @Query(value = """
            SELECT a.id
            FROM lms_accesses a
            WHERE a.project_id = :projectId AND a.group_id = :groupId AND a.role = 'champion'
            ORDER BY a.created_at ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findChampionAccessId(@Param("projectId") String projectId, @Param("groupId") Integer groupId);
}
