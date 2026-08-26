package com.ailene.lms.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "lms_quiz_submissions")
@Getter
@Setter
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @Column(name = "quiz_id", nullable = false)
    private String quizId;

    @Column(name = "attempt_number", nullable = false)
    private Short attemptNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> answers;

    @Column(nullable = false)
    private Short score;

    @Column(name = "is_completed", nullable = false)
    private Boolean completed;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;
}
