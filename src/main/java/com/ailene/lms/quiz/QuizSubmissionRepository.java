package com.ailene.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Integer> {

    Optional<QuizSubmission> findByStudentAccessIdAndQuizIdAndCompletedFalse(String studentAccessId, String quizId);

    Optional<QuizSubmission> findFirstByStudentAccessIdAndQuizIdAndCompletedTrueOrderByAttemptNumberDesc(
            String studentAccessId, String quizId);

    @Query(value = "SELECT MAX(attempt_number) FROM lms_quiz_submissions WHERE student_access_id = :accessId AND quiz_id = :quizId", nativeQuery = true)
    Short findMaxAttemptNumber(@Param("accessId") String accessId, @Param("quizId") String quizId);
}
