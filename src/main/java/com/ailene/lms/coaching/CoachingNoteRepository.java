package com.ailene.lms.coaching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoachingNoteRepository extends JpaRepository<CoachingNote, Integer> {

    @Query(value = """
            SELECT cn.text AS text,
                   cn.created_at AS createdAt,
                   su.id AS studentId,
                   su.full_name AS studentName,
                   su.avatar AS studentAvatar,
                   cu.id AS championId,
                   cu.full_name AS championName,
                   cu.avatar AS championAvatar
            FROM lms_coaching_notes cn
            JOIN lms_accesses sa ON sa.id = cn.student_access_id
            JOIN lms_users su ON su.id = sa.user_id
            JOIN lms_accesses ca ON ca.id = cn.champion_access_id
            JOIN lms_users cu ON cu.id = ca.user_id
            WHERE (:asChampion = true AND cn.champion_access_id = :accessId)
               OR (:asChampion = false AND cn.student_access_id = :accessId)
            ORDER BY cn.created_at DESC
            """, nativeQuery = true)
    List<CoachingNoteProjection> findNotes(@Param("accessId") String accessId,
            @Param("asChampion") boolean asChampion);

    @Query(value = """
            SELECT cn.text AS text,
                   cn.created_at AS createdAt,
                   su.id AS studentId,
                   su.full_name AS studentName,
                   su.avatar AS studentAvatar,
                   cu.id AS championId,
                   cu.full_name AS championName,
                   cu.avatar AS championAvatar
            FROM lms_coaching_notes cn
            JOIN lms_accesses sa ON sa.id = cn.student_access_id
            JOIN lms_users su ON su.id = sa.user_id
            JOIN lms_accesses ca ON ca.id = cn.champion_access_id
            JOIN lms_users cu ON cu.id = ca.user_id
            WHERE cn.id = :id
            """, nativeQuery = true)
    CoachingNoteProjection findNoteById(@Param("id") Integer id);
}
