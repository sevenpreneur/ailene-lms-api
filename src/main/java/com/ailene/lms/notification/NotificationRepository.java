package com.ailene.lms.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByAccessIdOrderByCreatedAtDescIdDesc(String accessId, Pageable pageable);

    Page<Notification> findByAccessIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(String accessId, Pageable pageable);

    long countByAccessIdAndReadAtIsNull(String accessId);

    @Modifying
    @Query(value = """
            UPDATE lms_notifications SET read_at = CURRENT_TIMESTAMP
            WHERE access_id = :accessId AND read_at IS NULL AND id IN (:ids)
            """, nativeQuery = true)
    int markRead(@Param("accessId") String accessId, @Param("ids") List<Integer> ids);

    @Modifying
    @Query(value = """
            UPDATE lms_notifications SET read_at = CURRENT_TIMESTAMP
            WHERE access_id = :accessId AND read_at IS NULL
            """, nativeQuery = true)
    int markAllRead(@Param("accessId") String accessId);

    @Query(value = """
            SELECT u.full_name FROM lms_accesses a JOIN lms_users u ON u.id = a.user_id WHERE a.id = :accessId
            """, nativeQuery = true)
    String findFullName(@Param("accessId") String accessId);

    // Same session pick as ChapterAccessGuard, so a group's own sitting wins over the all-groups one.
    @Modifying
    @Query(value = """
            INSERT INTO lms_notifications (access_id, type, title, body, ref_id, event_at, dedupe_key)
            SELECT a.id, CAST('chapter_reminder' AS lms_notification_type_enum), :title, c.name,
                   CAST(c.id AS VARCHAR), s.session_date, 'chapter_reminder:' || s.id
            FROM lms_accesses a
            JOIN lms_chapters c ON c.project_id = a.project_id AND c.status = 'active'
            JOIN LATERAL (
              SELECT s.id, s.session_date
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = c.id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)
              ORDER BY (s.only_group_id IS NULL), s.id
              LIMIT 1
            ) s ON true
            WHERE a.id = :accessId AND s.session_date > :now AND s.session_date <= :until
            ON CONFLICT (access_id, dedupe_key) DO NOTHING
            """, nativeQuery = true)
    int insertChapterReminders(@Param("accessId") String accessId, @Param("title") String title,
            @Param("now") OffsetDateTime now, @Param("until") OffsetDateTime until);

    @Modifying
    @Query(value = """
            INSERT INTO lms_notifications (access_id, type, title, body, ref_id, event_at, dedupe_key)
            SELECT ps.student_access_id, CAST('prompt_deadline' AS lms_notification_type_enum), :title, p.name,
                   CAST(p.id AS VARCHAR), ps.deadline, 'prompt_deadline:' || ps.id
            FROM lms_prompt_submissions ps
            JOIN lms_prompts p ON p.id = ps.prompt_id
            WHERE ps.student_access_id = :accessId AND ps.submitted_at IS NULL
              AND ps.deadline > :now AND ps.deadline <= :until
            ON CONFLICT (access_id, dedupe_key) DO NOTHING
            """, nativeQuery = true)
    int insertPromptDeadlineReminders(@Param("accessId") String accessId, @Param("title") String title,
            @Param("now") OffsetDateTime now, @Param("until") OffsetDateTime until);

    @Modifying
    @Query(value = """
            INSERT INTO lms_notifications (access_id, type, title, body, ref_id, event_at, dedupe_key)
            SELECT us.student_access_id, CAST('use_case_deadline' AS lms_notification_type_enum), :title, u.name,
                   CAST(u.id AS VARCHAR), us.deadline, 'use_case_deadline:' || us.id
            FROM lms_use_case_submissions us
            JOIN lms_use_cases u ON u.id = us.use_case_id
            WHERE us.student_access_id = :accessId AND us.submitted_at IS NULL
              AND us.deadline > :now AND us.deadline <= :until
            ON CONFLICT (access_id, dedupe_key) DO NOTHING
            """, nativeQuery = true)
    int insertUseCaseDeadlineReminders(@Param("accessId") String accessId, @Param("title") String title,
            @Param("now") OffsetDateTime now, @Param("until") OffsetDateTime until);

    // Only accesses this session actually applies to, so a group with its own sitting is skipped.
    @Modifying
    @Query(value = """
            INSERT INTO lms_notifications (access_id, type, title, body, ref_id, event_at, dedupe_key)
            SELECT a.id, CAST('chapter_unlocked' AS lms_notification_type_enum), :title, c.name,
                   CAST(c.id AS VARCHAR), t.session_date, 'chapter_unlocked:' || t.id
            FROM lms_chapter_sessions t
            JOIN lms_chapters c ON c.id = t.chapter_id AND c.status = 'active'
            JOIN lms_accesses a ON a.project_id = t.project_id AND a.role IN ('student', 'champion')
            JOIN LATERAL (
              SELECT s.id
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = t.chapter_id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = a.group_id)
              ORDER BY (s.only_group_id IS NULL), s.id
              LIMIT 1
            ) s ON s.id = t.id
            WHERE t.id = :sessionId AND t.is_locked = false
            ON CONFLICT (access_id, dedupe_key) DO NOTHING
            """, nativeQuery = true)
    int insertChapterUnlocked(@Param("sessionId") Integer sessionId, @Param("title") String title);
}
