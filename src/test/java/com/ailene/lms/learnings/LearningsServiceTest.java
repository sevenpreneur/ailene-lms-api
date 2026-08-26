package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.LevelMaterialProjection;
import com.ailene.lms.material.Material;
import com.ailene.lms.material.MaterialCompletionProjection;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.video.Video;
import com.ailene.lms.video.VideoCompletionProjection;
import com.ailene.lms.video.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

    @Test
    void levelMaterials_ordersAcrossChaptersAndLocksUnstartedSessions() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Material current = new Material();
        current.setId("mat-2");
        current.setChapterId(2);
        when(materialRepository.findById("mat-2")).thenReturn(Optional.of(current));

        Chapter chapter = new Chapter();
        chapter.setId(2);
        chapter.setLevelId(1);
        chapter.setName("Chapter 2");
        when(chapterRepository.findById(2)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(1);
        level.setProjectId("proj-1");
        level.setLevelNumber((short) 2);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 2);
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.of(status));

        LevelMaterialProjection row1 = mock(LevelMaterialProjection.class);
        when(row1.getMaterialId()).thenReturn("mat-1");
        when(row1.getMaterialTitle()).thenReturn("M1");
        when(row1.getSessionDate()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(row1.getCompleted()).thenReturn(true);

        LevelMaterialProjection row2 = mock(LevelMaterialProjection.class);
        when(row2.getMaterialId()).thenReturn("mat-2");
        when(row2.getMaterialTitle()).thenReturn("M2");
        when(row2.getSessionDate()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
        when(row2.getCompleted()).thenReturn(false);

        when(materialRepository.findLevelMaterials(1, "access-1")).thenReturn(List.of(row1, row2));

        LevelMaterialsResponse response = learningsService.getLevelMaterials("jwt",
                new LevelMaterialsRequest("mat-2"));

        assertThat(response.levelNumber()).isEqualTo((short) 2);
        assertThat(response.materials()).hasSize(2);

        LevelMaterialItem first = response.materials().get(0);
        assertThat(first.id()).isEqualTo("mat-1");
        assertThat(first.index()).isEqualTo(1);
        assertThat(first.completed()).isTrue();
        assertThat(first.locked()).isFalse();
        assertThat(first.isCurrent()).isFalse();

        LevelMaterialItem second = response.materials().get(1);
        assertThat(second.id()).isEqualTo("mat-2");
        assertThat(second.index()).isEqualTo(2);
        assertThat(second.completed()).isFalse();
        assertThat(second.locked()).isTrue();
        assertThat(second.isCurrent()).isTrue();
    }

    @Test
    void levelMaterials_levelNotYetUnlocked_locksEveryItem() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Material current = new Material();
        current.setId("mat-1");
        current.setChapterId(1);
        when(materialRepository.findById("mat-1")).thenReturn(Optional.of(current));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(3);
        chapter.setName("Chapter 1");
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(3);
        level.setProjectId("proj-1");
        level.setLevelNumber((short) 3);
        when(levelRepository.findById(3)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        LevelMaterialProjection row = mock(LevelMaterialProjection.class);
        when(row.getMaterialId()).thenReturn("mat-1");
        when(row.getMaterialTitle()).thenReturn("M1");
        when(row.getSessionDate()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(row.getCompleted()).thenReturn(false);
        when(materialRepository.findLevelMaterials(3, "access-1")).thenReturn(List.of(row));

        LevelMaterialsResponse response = learningsService.getLevelMaterials("jwt",
                new LevelMaterialsRequest("mat-1"));

        assertThat(response.materials()).hasSize(1);
        assertThat(response.materials().get(0).locked()).isTrue();
    }

    @Test
    void completeMaterial_firstCompletion_awardsXp() {
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

        when(materialRepository.insertXpEarning("mat-1", "access-1", (short) 10)).thenReturn(1);
        MaterialCompletionProjection completion = mock(MaterialCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(completion);

        MaterialCompletionResponse response = learningsService.completeMaterial("jwt",
                new MaterialCompletionRequest("mat-1"));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 10);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void completeMaterial_alreadyCompleted_awardsNoXp() {
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

        when(materialRepository.insertXpEarning("mat-1", "access-1", (short) 10)).thenReturn(0);
        MaterialCompletionProjection completion = mock(MaterialCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(completion);

        MaterialCompletionResponse response = learningsService.completeMaterial("jwt",
                new MaterialCompletionRequest("mat-1"));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 0);
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
        level.setProjectId("proj-1");
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        when(videoRepository.insertXpEarning(1, "access-1", (short) 15)).thenReturn(1);
        VideoCompletionProjection completion = mock(VideoCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(completion);

        VideoCompletionResponse response = learningsService.completeVideo("jwt", new VideoCompletionRequest(1));

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
        level.setProjectId("proj-1");
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        when(videoRepository.insertXpEarning(1, "access-1", (short) 15)).thenReturn(0);
        VideoCompletionProjection completion = mock(VideoCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(videoRepository.findCompletion(1, "access-1")).thenReturn(completion);

        VideoCompletionResponse response = learningsService.completeVideo("jwt", new VideoCompletionRequest(1));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 0);
    }
}
