package com.ailene.lms.quiz;

import com.ailene.lms.common.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lms_quizzes")
@Getter
@Setter
public class Quiz {

    @Id
    private String id;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "order_index", nullable = false)
    private Short orderIndex;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
