package com.ailene.lms.prompt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Integer> {

    @Query(value = """
            SELECT p.id AS id,
                   p.name AS name,
                   p.scenario AS description,
                   lv.level_number AS levelNumber
            FROM lms_prompts p
            JOIN lms_levels lv ON lv.id = p.level_id
            WHERE lv.project_id = :projectId
              AND p.status = 'active'
              AND p.is_self_created = false
              AND p.name ILIKE '%' || :search || '%'
            ORDER BY lv.level_number ASC, p.name ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<PromptListProjection> findPromptPage(@Param("projectId") String projectId, @Param("search") String search,
            @Param("limit") int limit, @Param("offset") long offset);

    @Query(value = """
            SELECT COUNT(*)
            FROM lms_prompts p
            JOIN lms_levels lv ON lv.id = p.level_id
            WHERE lv.project_id = :projectId
              AND p.status = 'active'
              AND p.is_self_created = false
              AND p.name ILIKE '%' || :search || '%'
            """, nativeQuery = true)
    long countPrompts(@Param("projectId") String projectId, @Param("search") String search);

    @Query(value = """
            SELECT pc.prompt_id AS promptId,
                   c.id AS id,
                   c.name AS name
            FROM lms_prompt_categories pc
            JOIN lms_categories c ON c.id = pc.category_id
            WHERE pc.prompt_id IN (:promptIds)
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<PromptCategoryProjection> findCategoriesForPrompts(@Param("promptIds") List<Integer> promptIds);

    @Query(value = """
            SELECT ps.prompt_id AS promptId,
                   ps.deadline AS deadlineAt,
                   ps.submitted_at AS submittedAt,
                   ps.is_accepted AS isAccepted
            FROM lms_prompt_submissions ps
            WHERE ps.student_access_id = :accessId AND ps.prompt_id IN (:promptIds)
            """, nativeQuery = true)
    List<PromptSubmissionProjection> findSubmissionsForAccess(@Param("accessId") String accessId,
            @Param("promptIds") List<Integer> promptIds);
}
