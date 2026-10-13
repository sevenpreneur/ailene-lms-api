package com.ailene.lms.chaptertest;

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
@Table(name = "lms_chapter_test_submissions")
@Getter
@Setter
public class ChapterTestSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @Column(name = "test_id", nullable = false)
    private Integer testId;

    // question id -> option_code for multiple choice, free text for essays
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> answers;

    @Column
    private Short score;

    @Column(name = "correct_count", nullable = false)
    private Short correctCount;

    @Column(name = "choice_count", nullable = false)
    private Short choiceCount;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;
}
