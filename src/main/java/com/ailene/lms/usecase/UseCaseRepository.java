package com.ailene.lms.usecase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UseCaseRepository extends JpaRepository<UseCase, Integer> {

    @Query(value = """
            SELECT u.id AS id,
                   u.name AS name,
                   u.description AS description,
                   lv.level_number AS levelNumber
            FROM lms_use_cases u
            JOIN lms_levels lv ON lv.id = u.level_id
            WHERE lv.project_id = :projectId
              AND u.status = 'active'
              AND u.is_self_created = false
              AND u.name ILIKE '%' || :search || '%'
            ORDER BY lv.level_number ASC, u.name ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<UseCaseListProjection> findUseCasePage(@Param("projectId") String projectId, @Param("search") String search,
            @Param("limit") int limit, @Param("offset") long offset);

    @Query(value = """
            SELECT COUNT(*)
            FROM lms_use_cases u
            JOIN lms_levels lv ON lv.id = u.level_id
            WHERE lv.project_id = :projectId
              AND u.status = 'active'
              AND u.is_self_created = false
              AND u.name ILIKE '%' || :search || '%'
            """, nativeQuery = true)
    long countUseCases(@Param("projectId") String projectId, @Param("search") String search);

    @Query(value = """
            SELECT uc.use_case_id AS useCaseId,
                   c.id AS id,
                   c.name AS name
            FROM lms_use_case_categories uc
            JOIN lms_categories c ON c.id = uc.category_id
            WHERE uc.use_case_id IN (:useCaseIds)
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<UseCaseCategoryProjection> findCategoriesForUseCases(@Param("useCaseIds") List<Integer> useCaseIds);

    @Query(value = """
            SELECT us.use_case_id AS useCaseId,
                   us.deadline AS deadlineAt,
                   us.submitted_at AS submittedAt,
                   us.is_accepted AS isAccepted
            FROM lms_use_case_submissions us
            WHERE us.student_access_id = :accessId AND us.use_case_id IN (:useCaseIds)
            """, nativeQuery = true)
    List<UseCaseSubmissionProjection> findSubmissionsForAccess(@Param("accessId") String accessId,
            @Param("useCaseIds") List<Integer> useCaseIds);

    @Query(value = """
            SELECT u.id AS id,
                   u.name AS name,
                   u.description AS description,
                   u.xp_reward AS xpReward,
                   us.deadline AS deadlineAt,
                   us.submitted_at AS submittedAt,
                   us.reviewed_at AS reviewedAt,
                   us.is_accepted AS isAccepted,
                   au.id AS assignedById,
                   au.full_name AS assignedByName,
                   au.avatar AS assignedByAvatar
            FROM lms_use_case_submissions us
            JOIN lms_use_cases u ON u.id = us.use_case_id
            JOIN lms_levels lv ON lv.id = u.level_id
            LEFT JOIN lms_accesses aa ON aa.id = us.assigned_by_access_id
            LEFT JOIN lms_users au ON au.id = aa.user_id
            WHERE lv.project_id = :projectId
              AND us.student_access_id = :accessId
              AND us.assigned_by_access_id IS NOT NULL
              AND (:hasSubmitted IS NULL OR (us.submitted_at IS NOT NULL) = :hasSubmitted)
              AND (:isAccepted IS NULL OR us.is_accepted = :isAccepted)
            ORDER BY us.deadline ASC
            """, nativeQuery = true)
    List<UseCaseAssignedProjection> findAssignedUseCases(@Param("projectId") String projectId,
            @Param("accessId") String accessId, @Param("hasSubmitted") Boolean hasSubmitted,
            @Param("isAccepted") Boolean isAccepted);
}
