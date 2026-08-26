package com.ailene.lms.video;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;
    private final VideoRepository videoRepository;

    public VideoDetailsResponse getVideoDetails(String jwt, VideoDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Video video = videoRepository.findById(request.videoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        Chapter chapter = resolveChapter(video.getChapterId());
        AccessContext ctx = resolveAccess(userId, chapter);
        requireLevelUnlocked(userId, ctx.level());
        Access access = ctx.access();

        var completion = videoRepository.findCompletion(video.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new VideoDetailsResponse(video.getId(), video.getTitle(), video.getDescription(), video.getVideoUrl(),
                video.getXpReward(), video.getOrderIndex(), new ChapterSummary(chapter.getId(), chapter.getName()),
                completedAt != null, TimeUtils.toOffsetDateTime(completedAt), video.getCreatedAt(),
                video.getUpdatedAt());
    }

    @Transactional
    public VideoCompletionResponse completeVideo(String jwt, VideoCompletionRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Video video = videoRepository.findById(request.videoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        Chapter chapter = resolveChapter(video.getChapterId());
        AccessContext ctx = resolveAccess(userId, chapter);
        requireLevelUnlocked(userId, ctx.level());
        Access access = ctx.access();

        videoRepository.insertCompletion(video.getId(), access.getId());
        int inserted = videoRepository.insertXpEarning(video.getId(), access.getId(), video.getXpReward());
        Short xpAwarded = inserted > 0 ? video.getXpReward() : 0;

        var completion = videoRepository.findCompletion(video.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new VideoCompletionResponse(video.getId(), true, TimeUtils.toOffsetDateTime(completedAt), xpAwarded);
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private AccessContext resolveAccess(UUID userId, Chapter chapter) {
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, level.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        return new AccessContext(access, level);
    }

    private void requireLevelUnlocked(UUID userId, Level level) {
        StudentStatusProjection status = accessRepository.findStudentStatus(userId, level.getProjectId())
                .orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();
        if (level.getLevelNumber() > currentLevelNumber) {
            throw new ForbiddenException("This level hasn't been unlocked yet.");
        }
    }

    private record AccessContext(Access access, Level level) {
    }
}
