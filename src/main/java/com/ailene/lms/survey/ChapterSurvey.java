package com.ailene.lms.survey;

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

@Entity
@Table(name = "lms_chapter_surveys")
@Getter
@Setter
public class ChapterSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "student_access_id", nullable = false, length = 21)
    private String studentAccessId;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    // the sitting being rated, which is what ties trainer_rating to a trainer
    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(nullable = false)
    private Short satisfaction;

    @Column(nullable = false)
    private Short relevance;

    @Column(name = "trainer_rating", nullable = false)
    private Short trainerRating;

    @Column
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;
}
