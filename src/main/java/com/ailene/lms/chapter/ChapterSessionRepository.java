package com.ailene.lms.chapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChapterSessionRepository extends JpaRepository<ChapterSession, Integer> {

    List<ChapterSession> findByChapterId(Integer chapterId);

    List<ChapterSession> findByProjectIdOrderBySessionDateAscIdAsc(String projectId);

    Optional<ChapterSession> findByIdAndProjectId(Integer id, String projectId);

    @Query(value = """
            SELECT s.*
            FROM lms_chapter_sessions s
            WHERE s.chapter_id = :chapterId AND s.status = 'active'
              AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
            """, nativeQuery = true)
    List<ChapterSession> findApplicableSessions(@Param("chapterId") Integer chapterId,
            @Param("accessId") String accessId);
}
