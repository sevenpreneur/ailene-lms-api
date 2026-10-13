package com.ailene.lms.chaptertest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterTestRepository extends JpaRepository<ChapterTest, Integer> {

    @Query(value = """
            SELECT t.id AS id,
                   t.kind::text AS kind,
                   t.name AS name,
                   t.description AS description,
                   (SELECT COUNT(*) FROM lms_chapter_test_questions tq WHERE tq.test_id = t.id) AS questionCount,
                   ts.score AS score,
                   ts.submitted_at AS submittedAt
            FROM lms_chapter_tests t
            LEFT JOIN lms_chapter_test_submissions ts ON ts.test_id = t.id AND ts.student_access_id = :accessId
            WHERE t.chapter_id = :chapterId AND t.status = 'active'
            """, nativeQuery = true)
    List<ChapterTestTaskProjection> findChapterTests(@Param("chapterId") Integer chapterId,
            @Param("accessId") String accessId);

    @Query(value = """
            SELECT tq.id AS questionId,
                   tq.type::text AS type,
                   tq.question AS question,
                   tq.explanation AS explanation,
                   tq.order_index AS orderIndex,
                   o.id AS optionId,
                   o.option_code AS optionCode,
                   o.text AS optionText,
                   o.is_correct AS optionIsCorrect
            FROM lms_chapter_test_questions tq
            LEFT JOIN lms_chapter_test_options o ON o.question_id = tq.id
            WHERE tq.test_id = :testId
            ORDER BY tq.order_index ASC, tq.id ASC, o.option_code ASC
            """, nativeQuery = true)
    List<ChapterTestQuestionOptionProjection> findQuestionsWithOptions(@Param("testId") Integer testId);

    @Query(value = """
            SELECT COUNT(*) FROM lms_materials m
            WHERE m.chapter_id = :chapterId AND m.status = 'active'
              AND NOT EXISTS (SELECT 1 FROM lms_material_completions mc
                              WHERE mc.material_id = m.id AND mc.student_access_id = :accessId)
            """, nativeQuery = true)
    long countIncompleteMaterials(@Param("chapterId") Integer chapterId, @Param("accessId") String accessId);
}
