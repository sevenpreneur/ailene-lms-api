package com.ailene.lms.chapter;

import com.ailene.lms.common.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "lms_chapter_sessions")
@Getter
@Setter
public class ChapterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "project_id", length = 21, nullable = false)
    private String projectId;

    // null means the session is open to every group in the project
    @Column(name = "only_group_id")
    private Integer onlyGroupId;

    @Column(name = "session_date", nullable = false)
    private OffsetDateTime sessionDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "location_url", nullable = false)
    private String locationUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ChapterMethod method;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "trainer_id", length = 21)
    private String trainerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
