package com.ailene.lms.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AccessRepository extends JpaRepository<Access, String> {

    @Query(value = """
            SELECT p.id AS projectId,
                   p.name AS projectName,
                   c.image_url AS projectAvatar,
                   a.role AS role
            FROM lms_accesses a
            JOIN lms_projects p ON p.id = a.project_id
            LEFT JOIN b2b_company c ON c.id = p.company_id
            WHERE a.user_id = :userId
            """, nativeQuery = true)
    List<ProjectAccessProjection> findProjectAccessByUserId(@Param("userId") UUID userId);
}
