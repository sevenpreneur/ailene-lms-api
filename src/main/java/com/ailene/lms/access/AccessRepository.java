package com.ailene.lms.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessRepository extends JpaRepository<Access, String> {

    @Query(value = """
            SELECT p.id AS id,
                   p.name AS name,
                   c.image_url AS avatar,
                   g.id AS groupId,
                   g.name AS groupName,
                   a.role AS role
            FROM lms_accesses a
            JOIN lms_projects p ON p.id = a.project_id
            LEFT JOIN b2b_company c ON c.id = p.company_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            WHERE a.user_id = :userId
            """, nativeQuery = true)
    List<ProjectAccessProjection> findProjectAccessByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT (SELECT COALESCE(SUM(xp_earned), 0) FROM lms_xp_earnings WHERE student_access_id = a.id) AS xpCount,
                   lv.level_number AS currentLevelNumber,
                   EXISTS (SELECT 1 FROM lms_pre_assessments pa WHERE pa.access_id = a.id) AS hasPreAssessment
            FROM lms_accesses a
            LEFT JOIN lms_levels lv ON lv.id = a.current_level_id AND lv.project_id = a.project_id
            WHERE a.user_id = :userId AND a.project_id = :projectId
            """, nativeQuery = true)
    Optional<StudentStatusProjection> findStudentStatus(@Param("userId") UUID userId,
            @Param("projectId") String projectId);
}
