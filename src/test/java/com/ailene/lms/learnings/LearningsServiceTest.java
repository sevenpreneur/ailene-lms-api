package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.Material;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.video.Video;
import com.ailene.lms.video.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningsServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LevelRepository levelRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private LearningsService learningsService;

    // findCompletion() returns null (not a projection with a null getter) when the student has no completion row.
    @Test
    void materialDetails_uncompletedMaterial_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Material material = new Material();
        material.setId("mat-1");
        material.setChapterId(1);
        material.setTitle("Title");
        material.setXpReward((short) 10);
        material.setOrderIndex((short) 0);
        when(materialRepository.findById("mat-1")).thenReturn(Optional.of(material));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(1);
        chapter.setName("Chapter");
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        level.setProjectId("proj-1");
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(null);

        MaterialDetailsResponse response = learningsService.getMaterialDetails("jwt",
                new MaterialDetailsRequest("mat-1"));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
    }

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
        level.setProjectId("proj-1");
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        when(videoRepository.findCompletion(1, "access-1")).thenReturn(null);

        VideoDetailsResponse response = learningsService.getVideoDetails("jwt", new VideoDetailsRequest(1));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
    }
}
