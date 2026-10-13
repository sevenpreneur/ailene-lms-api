package com.ailene.lms.survey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChapterSurveyRepository extends JpaRepository<ChapterSurvey, Integer> {

    Optional<ChapterSurvey> findByStudentAccessIdAndChapterId(String studentAccessId, Integer chapterId);

    @Query(value = """
            SELECT s.id AS id,
                   s.session_date AS sessionDate
            FROM lms_chapter_sessions s
            WHERE s.chapter_id = :chapterId AND s.status = 'active'
              AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
            ORDER BY (s.only_group_id IS NULL)
            LIMIT 1
            """, nativeQuery = true)
    Optional<ChapterSessionSlotProjection> findApplicableSession(@Param("chapterId") Integer chapterId,
            @Param("accessId") String accessId);
}
