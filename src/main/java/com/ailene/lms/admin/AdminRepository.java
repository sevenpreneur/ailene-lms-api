package com.ailene.lms.admin;

import com.ailene.lms.access.Access;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends Repository<Access, String> {

    // The one read of the identity service's tables: admin sessions are its tokens, not lms_tokens.
    @Query(value = """
            SELECT u.id AS userId,
                   u.email AS email,
                   u.role::text AS role,
                   u.status::text AS status,
                   (u.deleted_at IS NOT NULL) AS deleted
            FROM tokens t
            JOIN users u ON u.id = t.user_id
            WHERE t.token = :token AND t.is_active = true
            """, nativeQuery = true)
    Optional<AdminTokenOwnerProjection> findActiveTokenOwner(@Param("token") String token);

    @Query(value = """
            SELECT p.id AS id,
                   p.name AS name,
                   p.company_name AS companyName,
                   (SELECT COUNT(*) FROM lms_groups g WHERE g.project_id = p.id) AS groupCount,
                   (SELECT COUNT(*) FROM lms_accesses a WHERE a.project_id = p.id) AS memberCount
            FROM lms_projects p
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<AdminProjectProjection> findProjects();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM lms_projects WHERE id = :projectId)", nativeQuery = true)
    boolean projectExists(@Param("projectId") String projectId);

    @Query(value = "SELECT name FROM lms_projects WHERE id = :projectId", nativeQuery = true)
    Optional<String> findProjectName(@Param("projectId") String projectId);

    @Query(value = """
            SELECT a.id AS accessId,
                   a.role::text AS role,
                   a.group_id AS groupId,
                   g.name AS groupName,
                   a.created_at AS joinedAt,
                   u.id AS userId,
                   u.full_name AS fullName,
                   u.email AS email,
                   u.avatar AS avatar,
                   u.job_title AS jobTitle,
                   u.last_active_at AS lastActiveAt
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            JOIN lms_groups g ON g.id = a.group_id
            WHERE a.project_id = :projectId
              AND (CAST(:groupId AS INTEGER) IS NULL OR a.group_id = CAST(:groupId AS INTEGER))
              AND (CAST(:accessId AS TEXT) IS NULL OR a.id = CAST(:accessId AS TEXT))
            ORDER BY g.name, u.full_name
            """, nativeQuery = true)
    List<AdminMemberProjection> findMembers(@Param("projectId") String projectId, @Param("groupId") Integer groupId,
            @Param("accessId") String accessId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM lms_accesses WHERE project_id = :projectId AND user_id = :userId)",
            nativeQuery = true)
    boolean accessExists(@Param("projectId") String projectId, @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO lms_accesses (id, project_id, user_id, group_id, role)
            VALUES (:id, :projectId, :userId, :groupId, CAST(:role AS lms_access_role_enum))
            """, nativeQuery = true)
    int insertAccess(@Param("id") String id, @Param("projectId") String projectId, @Param("userId") UUID userId,
            @Param("groupId") Integer groupId, @Param("role") String role);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE lms_accesses
            SET role = CAST(:role AS lms_access_role_enum), group_id = :groupId, updated_at = CURRENT_TIMESTAMP
            WHERE id = :accessId AND project_id = :projectId
            """, nativeQuery = true)
    int updateAccess(@Param("projectId") String projectId, @Param("accessId") String accessId,
            @Param("role") String role, @Param("groupId") Integer groupId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lms_accesses WHERE id = :accessId AND project_id = :projectId", nativeQuery = true)
    int deleteAccess(@Param("projectId") String projectId, @Param("accessId") String accessId);

    default Optional<AdminMemberProjection> findMember(String projectId, String accessId) {
        return findMembers(projectId, null, accessId).stream().findFirst();
    }
}
