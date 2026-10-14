package com.ailene.lms.video;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private VideoService videoService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Video video = new Video();
        video.setId(1);
        video.setChapterId(1);
        video.setTitle("Title");
        video.setXpReward((short) 15);
        video.setOrderIndex((short) 0);
        when(videoRepository.findById(1)).thenReturn(Optional.of(video));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(1);
        chapter.setName("Chapter");
        chapter.setProjectId("proj-1");
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    // findCompletion() returns null (not a projection with a null getter) when the student has no completion row.
    @Test
    void videoDetails_uncompletedVideo_doesNotThrow() {
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(null);

        VideoDetailsResponse response = videoService.getVideoDetails("jwt", new VideoDetailsRequest(1));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
        verify(chapterAccessGuard).requireUnlocked(1, "access-1");
    }

    @Test
    void videoDetails_sessionNotStarted_throwsForbidden() {
        when(chapterAccessGuard.requireUnlocked(1, "access-1")).thenThrow(
                new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.session_not_started)));

        assertThatThrownBy(() -> videoService.getVideoDetails("jwt", new VideoDetailsRequest(1)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void completeVideo_adminLocked_throwsForbiddenAndWritesNothing() {
        when(chapterAccessGuard.requireUnlocked(1, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(ChapterLockedReason.admin_locked)));

        assertThatThrownBy(() -> videoService.completeVideo("jwt", new VideoCompletionRequest(1)))
                .isInstanceOf(ForbiddenException.class);
        verify(videoRepository, never()).insertCompletion(anyInt(), anyString());
        verify(videoRepository, never()).insertXpEarning(anyInt(), anyString(), any());
    }

    @Test
    void completeVideo_firstCompletion_awardsXp() {
        when(videoRepository.insertXpEarning(1, "access-1", (short) 15)).thenReturn(1);
        VideoCompletionProjection completion = mock(VideoCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(completion);

        VideoCompletionResponse response = videoService.completeVideo("jwt", new VideoCompletionRequest(1));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 15);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void completeVideo_alreadyCompleted_awardsNoXp() {
        when(videoRepository.insertXpEarning(1, "access-1", (short) 15)).thenReturn(0);
        VideoCompletionProjection completion = mock(VideoCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(completion);

        VideoCompletionResponse response = videoService.completeVideo("jwt", new VideoCompletionRequest(1));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 0);
    }
}
