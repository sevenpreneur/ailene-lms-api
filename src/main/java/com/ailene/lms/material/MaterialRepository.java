package com.ailene.lms.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, String> {

    @Query(value = """
            SELECT m.id AS id,
                   m.title AS title,
                   m.description AS description,
                   m.xp_reward AS xpReward,
                   m.order_index AS orderIndex,
                   EXISTS (SELECT 1 FROM lms_material_completions mc
                      WHERE mc.student_access_id = :accessId AND mc.material_id = m.id) AS completed
            FROM lms_materials m
            WHERE m.chapter_id = :chapterId AND m.status = 'active'
            ORDER BY m.order_index ASC
            """, nativeQuery = true)
    List<MaterialTaskProjection> findMaterialTasks(@Param("chapterId") Integer chapterId,
            @Param("accessId") String accessId);

    @Query(value = """
            SELECT (SELECT mc.completed_at FROM lms_material_completions mc
                      WHERE mc.student_access_id = :accessId AND mc.material_id = :materialId) AS completedAt
            """, nativeQuery = true)
    MaterialCompletionProjection findCompletion(@Param("materialId") String materialId,
            @Param("accessId") String accessId);

    @Query(value = """
            SELECT s.session_date AS sessionDate,
                   m.id AS materialId,
                   m.title AS materialTitle,
                   m.order_index AS orderIndex,
                   EXISTS (SELECT 1 FROM lms_material_completions mc
                      WHERE mc.student_access_id = :accessId AND mc.material_id = m.id) AS completed
            FROM lms_chapters c
            JOIN lms_materials m ON m.chapter_id = c.id AND m.status = 'active'
            JOIN LATERAL (
              SELECT s.session_date
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = c.id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
              ORDER BY (s.only_group_id IS NULL)
              LIMIT 1
            ) s ON true
            WHERE c.level_id = :levelId AND c.status = 'active'
            ORDER BY s.session_date ASC, m.order_index ASC
            """, nativeQuery = true)
    List<LevelMaterialProjection> findLevelMaterials(@Param("levelId") Integer levelId,
            @Param("accessId") String accessId);

    @Query(value = """
            SELECT c.id AS chapterId,
                   m.id AS materialId,
                   m.title AS title,
                   m.order_index AS orderIndex,
                   EXISTS (SELECT 1 FROM lms_material_completions mc
                      WHERE mc.student_access_id = :accessId AND mc.material_id = m.id) AS completed
            FROM lms_materials m
            JOIN lms_chapters c ON c.id = m.chapter_id AND c.status = 'active'
            JOIN lms_levels lv ON lv.id = c.level_id
            JOIN LATERAL (
              SELECT s.session_date
              FROM lms_chapter_sessions s
              WHERE s.chapter_id = c.id AND s.status = 'active'
                AND (s.only_group_id IS NULL OR s.only_group_id = (SELECT a.group_id FROM lms_accesses a WHERE a.id = :accessId))
              ORDER BY (s.only_group_id IS NULL)
              LIMIT 1
            ) s ON true
            WHERE c.project_id = :projectId AND m.status = 'active'
            ORDER BY s.session_date ASC, m.order_index ASC
            """, nativeQuery = true)
    List<MaterialFocusProjection> findProjectMaterialsForFocus(@Param("projectId") String projectId,
            @Param("accessId") String accessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_material_completions (student_access_id, material_id)
            VALUES (:accessId, :materialId)
            ON CONFLICT (student_access_id, material_id) DO NOTHING
            """, nativeQuery = true)
    int insertCompletion(@Param("materialId") String materialId, @Param("accessId") String accessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_xp_earnings (student_access_id, learning_type, learning_id, xp_earned)
            VALUES (:accessId, 'material', :materialId, :xpEarned)
            ON CONFLICT (student_access_id, learning_type, learning_id) DO NOTHING
            """, nativeQuery = true)
    int insertXpEarning(@Param("materialId") String materialId, @Param("accessId") String accessId,
            @Param("xpEarned") Short xpEarned);
}
