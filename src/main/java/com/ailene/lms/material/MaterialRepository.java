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
