package com.ailene.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, String> {

    @Query(value = """
            SELECT q.id AS id,
                   q.name AS name,
                   q.description AS description,
                   q.order_index AS orderIndex,
                   (SELECT COUNT(*) FROM lms_quiz_questions qq WHERE qq.quiz_id = q.id) AS questionCount,
                   COALESCE((SELECT SUM(qq.xp_reward) FROM lms_quiz_questions qq WHERE qq.quiz_id = q.id), 0) AS xpReward,
                   COALESCE((SELECT xe.xp_earned FROM lms_xp_earnings xe
                      WHERE xe.student_access_id = :accessId AND xe.learning_type = 'quiz' AND xe.learning_id = q.id), 0) AS xpEarned,
                   (SELECT MAX(qs.score) FROM lms_quiz_submissions qs
                      WHERE qs.student_access_id = :accessId AND qs.quiz_id = q.id AND qs.is_completed = true) AS bestScore,
                   (SELECT COUNT(*) FROM lms_quiz_submissions qs
                      WHERE qs.student_access_id = :accessId AND qs.quiz_id = q.id AND qs.is_completed = true) AS attempts,
                   (SELECT MAX(qs.started_at) FROM lms_quiz_submissions qs
                      WHERE qs.student_access_id = :accessId AND qs.quiz_id = q.id AND qs.is_completed = false) AS activeAttemptStartedAt
            FROM lms_quizzes q
            WHERE q.chapter_id = :chapterId AND q.status = 'active'
            ORDER BY q.order_index ASC
            """, nativeQuery = true)
    List<QuizTaskProjection> findQuizTasks(@Param("chapterId") Integer chapterId, @Param("accessId") String accessId);

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM lms_quiz_questions qq WHERE qq.quiz_id = :quizId) AS questionCount,
                   COALESCE((SELECT SUM(qq.xp_reward) FROM lms_quiz_questions qq WHERE qq.quiz_id = :quizId), 0) AS xpReward,
                   (SELECT COUNT(*) FROM lms_quiz_submissions qs
                      WHERE qs.student_access_id = :accessId AND qs.quiz_id = :quizId AND qs.is_completed = true) AS attempts
            """, nativeQuery = true)
    QuizStatsProjection findQuizStats(@Param("quizId") String quizId, @Param("accessId") String accessId);

    @Query(value = """
            SELECT qq.id AS questionId,
                   qq.question AS question,
                   qq.order_index AS questionOrderIndex,
                   qq.xp_reward AS questionXpReward,
                   qo.id AS optionId,
                   qo.option_code AS optionCode,
                   qo.text AS optionText
            FROM lms_quiz_questions qq
            JOIN lms_quiz_options qo ON qo.question_id = qq.id
            WHERE qq.quiz_id = :quizId
            ORDER BY qq.order_index ASC, qo.option_code ASC
            """, nativeQuery = true)
    List<QuizQuestionOptionProjection> findQuestionsWithOptions(@Param("quizId") String quizId);

    @Query(value = """
            SELECT xe.xp_earned FROM lms_xp_earnings xe
            WHERE xe.student_access_id = :accessId AND xe.learning_type = 'quiz' AND xe.learning_id = :quizId
            """, nativeQuery = true)
    Short findXpEarned(@Param("quizId") String quizId, @Param("accessId") String accessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_xp_earnings (student_access_id, learning_type, learning_id, xp_earned)
            VALUES (:accessId, 'quiz', :quizId, :xpEarned)
            ON CONFLICT (student_access_id, learning_type, learning_id)
            DO UPDATE SET xp_earned = EXCLUDED.xp_earned, earned_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertXpEarning(@Param("quizId") String quizId, @Param("accessId") String accessId,
            @Param("xpEarned") Short xpEarned);

    @Query(value = """
            SELECT qq.id AS questionId,
                   qq.xp_reward AS xpReward,
                   (SELECT qo.option_code FROM lms_quiz_options qo
                      WHERE qo.question_id = qq.id AND qo.is_correct = true LIMIT 1) AS correctOptionCode
            FROM lms_quiz_questions qq
            WHERE qq.quiz_id = :quizId
            """, nativeQuery = true)
    List<QuizAnswerKeyProjection> findAnswerKey(@Param("quizId") String quizId);

    @Query(value = """
            SELECT qq.id AS questionId,
                   qq.question AS question,
                   qq.order_index AS questionOrderIndex,
                   qq.xp_reward AS questionXpReward,
                   qq.explanation AS explanation,
                   qo.id AS optionId,
                   qo.option_code AS optionCode,
                   qo.text AS optionText,
                   qo.is_correct AS optionIsCorrect
            FROM lms_quiz_questions qq
            JOIN lms_quiz_options qo ON qo.question_id = qq.id
            WHERE qq.quiz_id = :quizId
            ORDER BY qq.order_index ASC, qo.option_code ASC
            """, nativeQuery = true)
    List<QuizResultQuestionOptionProjection> findQuestionsWithAnswerKey(@Param("quizId") String quizId);

    @Query(value = """
            SELECT AVG(best_score) FROM (
              SELECT qs.quiz_id, MAX(qs.score) AS best_score
              FROM lms_quiz_submissions qs
              JOIN lms_quizzes q ON q.id = qs.quiz_id
              JOIN lms_chapters c ON c.id = q.chapter_id
              JOIN lms_levels lv ON lv.id = c.level_id
              WHERE lv.project_id = :projectId AND qs.student_access_id = :accessId AND qs.is_completed = true
              GROUP BY qs.quiz_id
            ) t
            """, nativeQuery = true)
    Double findAverageBestScore(@Param("projectId") String projectId, @Param("accessId") String accessId);
}
