package com.ailene.lms.preassessment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreAssessmentBaselineRepository extends Repository<PreAssessmentAttempt, Integer> {

    // Every non-sponsor access, with its attempt when there is one; a null groupId means the whole project.
    @Query(value = """
            SELECT a.id AS accessId,
                   a.group_id AS groupId,
                   g.name AS groupName,
                   u.full_name AS fullName,
                   u.avatar AS avatar,
                   pa.status::text AS status,
                   pa.final_band::int AS band,
                   pa.submitted_at AS submittedAt
            FROM lms_accesses a
            JOIN lms_users u ON u.id = a.user_id
            LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
            LEFT JOIN lms_pa_attempts pa ON pa.access_id = a.id
            WHERE a.project_id = :projectId AND a.role <> 'sponsor'
              AND (CAST(:groupId AS INTEGER) IS NULL OR a.group_id = CAST(:groupId AS INTEGER))
            ORDER BY u.full_name, a.id
            """, nativeQuery = true)
    List<PreAssessmentBaselineMemberProjection> findMembers(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId);

    // Cluster score = item-weighted mean of its domains, the same rule as pre-assessment/score.
    @Query(value = """
            SELECT pa.access_id AS accessId,
                   c.code AS clusterCode,
                   SUM(s.score * s.item_count) / NULLIF(SUM(s.item_count), 0) AS score
            FROM lms_pa_attempts pa
            JOIN lms_accesses a ON a.id = pa.access_id
            JOIN lms_pa_attempt_domain_scores s ON s.attempt_id = pa.id
            JOIN lms_pa_domains d ON d.id = s.domain_id
            JOIN lms_pa_clusters c ON c.id = d.cluster_id
            WHERE a.project_id = :projectId AND a.role <> 'sponsor' AND pa.status = 'scored'
              AND (CAST(:groupId AS INTEGER) IS NULL OR a.group_id = CAST(:groupId AS INTEGER))
            GROUP BY pa.access_id, c.code
            """, nativeQuery = true)
    List<PreAssessmentBaselineClusterScoreProjection> findClusterScores(@Param("projectId") String projectId,
            @Param("groupId") Integer groupId);

    @Query(value = """
            SELECT c.code AS code, c.name AS name
            FROM lms_pa_clusters c
            ORDER BY c.order_index, c.id
            """, nativeQuery = true)
    List<PreAssessmentClusterProjection> findClusters();
}
