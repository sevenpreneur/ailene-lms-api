package com.ailene.lms.champion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentDraftRepository extends JpaRepository<AssignmentDraft, Integer> {

    @Query(value = """
            SELECT batch_id
            FROM lms_assignment_drafts
            WHERE champion_access_id = :championAccessId
            GROUP BY batch_id
            ORDER BY MAX(created_at) DESC, MAX(id) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findRecentBatchIds(@Param("championAccessId") String championAccessId, @Param("limit") int limit);

    List<AssignmentDraft> findByChampionAccessIdAndBatchIdInOrderByIdAsc(String championAccessId,
            Collection<String> batchIds);

    Optional<AssignmentDraft> findByIdAndChampionAccessId(Integer id, String championAccessId);
}
