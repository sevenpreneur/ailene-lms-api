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

    // the sitting being rated, which is what ties the trainer_* ratings to a trainer
    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(name = "trainer_clarity", nullable = false)
    private Short trainerClarity;

    @Column(name = "trainer_mastery", nullable = false)
    private Short trainerMastery;

    @Column(name = "trainer_responsiveness", nullable = false)
    private Short trainerResponsiveness;

    @Column(name = "trainer_engagement", nullable = false)
    private Short trainerEngagement;

    @Column(name = "material_relevance", nullable = false)
    private Short materialRelevance;

    @Column(name = "material_flow", nullable = false)
    private Short materialFlow;

    @Column(name = "material_depth", nullable = false)
    private Short materialDepth;

    @Column(name = "learning_value", nullable = false)
    private Short learningValue;

    @Column(name = "missing_topics")
    private String missingTopics;

    @Column(name = "favorite_material")
    private String favoriteMaterial;

    @Column(name = "disliked_material")
    private String dislikedMaterial;

    @Column(name = "improvement_suggestion")
    private String improvementSuggestion;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;
}
