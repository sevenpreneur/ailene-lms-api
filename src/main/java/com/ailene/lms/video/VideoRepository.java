package com.ailene.lms.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Integer> {

    @Query(value = """
            SELECT v.id AS id,
                   v.title AS title,
                   v.description AS description,
                   v.video_url AS videoUrl,
                   v.xp_reward AS xpReward,
                   v.order_index AS orderIndex,
                   COALESCE((SELECT xe.xp_earned FROM lms_xp_earnings xe
                      WHERE xe.student_access_id = :accessId AND xe.learning_type = 'video' AND xe.learning_id = v.id::text), 0) AS xpEarned,
                   EXISTS (SELECT 1 FROM lms_video_completions vc
                      WHERE vc.student_access_id = :accessId AND vc.video_id = v.id) AS completed
            FROM lms_videos v
            WHERE v.chapter_id = :chapterId AND v.status = 'active'
            ORDER BY v.order_index ASC
            """, nativeQuery = true)
    List<VideoTaskProjection> findVideoTasks(@Param("chapterId") Integer chapterId, @Param("accessId") String accessId);

    @Query(value = """
            SELECT (SELECT vc.completed_at FROM lms_video_completions vc
                      WHERE vc.student_access_id = :accessId AND vc.video_id = :videoId) AS completedAt
            """, nativeQuery = true)
    VideoCompletionProjection findCompletion(@Param("videoId") Integer videoId, @Param("accessId") String accessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_video_completions (student_access_id, video_id)
            VALUES (:accessId, :videoId)
            ON CONFLICT (student_access_id, video_id) DO NOTHING
            """, nativeQuery = true)
    int insertCompletion(@Param("videoId") Integer videoId, @Param("accessId") String accessId);

    @Modifying
    @Query(value = """
            INSERT INTO lms_xp_earnings (student_access_id, learning_type, learning_id, xp_earned)
            VALUES (:accessId, 'video', CAST(:videoId AS text), :xpEarned)
            ON CONFLICT (student_access_id, learning_type, learning_id) DO NOTHING
            """, nativeQuery = true)
    int insertXpEarning(@Param("videoId") Integer videoId, @Param("accessId") String accessId,
            @Param("xpEarned") Short xpEarned);
}
