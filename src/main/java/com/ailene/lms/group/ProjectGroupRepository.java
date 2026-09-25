package com.ailene.lms.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, Integer> {

    Optional<ProjectGroup> findByIdAndProjectId(Integer id, String projectId);

    boolean existsByProjectIdAndNameIgnoreCase(String projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(String projectId, String name, Integer id);

    @Query(value = """
            SELECT g.id AS id,
                   g.name AS name,
                   (SELECT COUNT(*) FROM lms_accesses a WHERE a.group_id = g.id) AS memberCount,
                   g.created_at AS createdAt
            FROM lms_groups g
            WHERE g.project_id = :projectId
            ORDER BY g.name, g.id
            """, nativeQuery = true)
    List<GroupListProjection> findGroupList(@Param("projectId") String projectId);

    @Query(value = "SELECT COUNT(*) FROM lms_accesses WHERE group_id = :groupId", nativeQuery = true)
    long countMembers(@Param("groupId") Integer groupId);

    @Query(value = "SELECT COUNT(*) FROM lms_chapter_sessions WHERE only_group_id = :groupId", nativeQuery = true)
    long countSessions(@Param("groupId") Integer groupId);
}
