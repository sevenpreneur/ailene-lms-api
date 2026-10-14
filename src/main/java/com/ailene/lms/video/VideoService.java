package com.ailene.lms.video;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ResourceNotFoundException;
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
    private final VideoRepository videoRepository;
    private final ChapterAccessGuard chapterAccessGuard;

    public VideoDetailsResponse getVideoDetails(String jwt, VideoDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Video video = videoRepository.findById(request.videoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        Chapter chapter = resolveChapter(video.getChapterId());
        Access access = resolveAccess(userId, chapter);
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

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
        Access access = resolveAccess(userId, chapter);
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

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

    private Access resolveAccess(UUID userId, Chapter chapter) {
        return accessRepository.findByUserIdAndProjectId(userId, chapter.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
