package com.ailene.lms.chapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query(value = """
            SELECT c.id AS id,
                   c.name AS name,
                   c.description AS description,
                   s.session_date AS sessionDate,
                   s.duration_minutes AS durationMinutes,
                   s.location_name AS locationName,
                   s.location_url AS locationUrl,
                   s.method AS method,
                   lv.id AS levelId,
                   lv.level_number AS levelNumber,
                   lv.name AS levelName,
                   (
                     (SELECT COUNT(*) FROM lms_quizzes q WHERE q.chapter_id = c.id AND q.status = 'active') +
                     (SELECT COUNT(*) FROM lms_videos v WHERE v.chapter_id = c.id AND v.status = 'active') +
                     (SELECT COUNT(*) FROM lms_materials m WHERE m.chapter_id = c.id AND m.status = 'active')
                   ) AS totalTasks,
                   (
                     (SELECT COUNT(DISTINCT qs.quiz_id) FROM lms_quiz_submissions qs
                        JOIN lms_quizzes q ON q.id = qs.quiz_id
                        WHERE qs.student_access_id = :accessId AND qs.is_completed = true
                          AND q.chapter_id = c.id AND q.status = 'active') +
                     (SELECT COUNT(*) FROM lms_video_completions vc
                        JOIN lms_videos v ON v.id = vc.video_id
                        WHERE vc.student_access_id = :accessId AND v.chapter_id = c.id AND v.status = 'active') +
                     (SELECT COUNT(*) FROM lms_material_completions mc
                        JOIN lms_materials m ON m.id = mc.material_id
                        WHERE mc.student_access_id = :accessId AND m.chapter_id = c.id AND m.status = 'active')
                   ) AS doneTasks
            FROM lms_chapters c
            JOIN lms_levels lv ON lv.id = c.level_id
            JOIN LATERAL (
              SELECT s.session_date, s.duration_minutes, s.location_name, s.location_url, s.method
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = c.id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
              ORDER BY (s.only_group_id IS NULL)
              LIMIT 1
            ) s ON true
            WHERE c.project_id = :projectId AND c.status = 'active'
            ORDER BY s.session_date ASC
            """, nativeQuery = true)
    List<ChapterListProjection> findChapterList(@Param("projectId") String projectId,
            @Param("accessId") String accessId);

    @Query(value = """
            SELECT c.name
            FROM lms_chapters c
            JOIN lms_levels lv ON lv.id = c.level_id
            JOIN LATERAL (
              SELECT s.session_date, s.duration_minutes, s.location_name, s.location_url, s.method
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = c.id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
              ORDER BY (s.only_group_id IS NULL)
              LIMIT 1
            ) s ON true
            WHERE c.project_id = :projectId AND c.status = 'active'
            ORDER BY c.id ASC
            """, nativeQuery = true)
    List<String> findActiveChapterNames(@Param("projectId") String projectId,
            @Param("accessId") String accessId);
}
