package com.ailene.lms.video;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LevelRepository levelRepository;
    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoService videoService;

    // findCompletion() returns null (not a projection with a null getter) when the student has no completion row.
    @Test
    void videoDetails_uncompletedVideo_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Video video = new Video();
        video.setId(1);
        video.setChapterId(1);
        video.setTitle("Title");
        video.setXpReward((short) 10);
        video.setOrderIndex((short) 0);
        when(videoRepository.findById(1)).thenReturn(Optional.of(video));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(1);
        chapter.setName("Chapter");
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        chapter.setProjectId("proj-1");
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(videoRepository.findCompletion(1, "access-1")).thenReturn(null);

        VideoDetailsResponse response = videoService.getVideoDetails("jwt", new VideoDetailsRequest(1));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void videoDetails_levelNotUnlocked_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Video video = new Video();
        video.setId(1);
        video.setChapterId(1);
        when(videoRepository.findById(1)).thenReturn(Optional.of(video));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(2);
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(2);
        chapter.setProjectId("proj-1");
        level.setLevelNumber((short) 2);
        when(levelRepository.findById(2)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.of(status));

        assertThatThrownBy(() -> videoService.getVideoDetails("jwt", new VideoDetailsRequest(1)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void completeVideo_firstCompletion_awardsXp() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

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
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        chapter.setProjectId("proj-1");
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

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
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

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
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        chapter.setProjectId("proj-1");
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(videoRepository.insertXpEarning(1, "access-1", (short) 15)).thenReturn(0);
        VideoCompletionProjection completion = mock(VideoCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(completion);

        VideoCompletionResponse response = videoService.completeVideo("jwt", new VideoCompletionRequest(1));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 0);
    }
}
