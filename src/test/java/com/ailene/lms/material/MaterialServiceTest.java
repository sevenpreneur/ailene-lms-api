package com.ailene.lms.material;

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
class MaterialServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private LevelRepository levelRepository;
    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialService materialService;

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
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(null);

        MaterialDetailsResponse response = materialService.getMaterialDetails("jwt",
                new MaterialDetailsRequest("mat-1"));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void materialDetails_levelNotUnlocked_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Material material = new Material();
        material.setId("mat-1");
        material.setChapterId(1);
        when(materialRepository.findById("mat-1")).thenReturn(Optional.of(material));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(2);
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(2);
        level.setProjectId("proj-1");
        level.setLevelNumber((short) 2);
        when(levelRepository.findById(2)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.of(status));

        assertThatThrownBy(() -> materialService.getMaterialDetails("jwt", new MaterialDetailsRequest("mat-1")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void completeMaterial_levelNotUnlocked_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Material material = new Material();
        material.setId("mat-1");
        material.setChapterId(1);
        material.setXpReward((short) 10);
        when(materialRepository.findById("mat-1")).thenReturn(Optional.of(material));

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(2);
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Level level = new Level();
        level.setId(2);
        level.setProjectId("proj-1");
        level.setLevelNumber((short) 2);
        when(levelRepository.findById(2)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));

        StudentStatusProjection status = mock(StudentStatusProjection.class);
        when(status.getCurrentLevelNumber()).thenReturn((short) 1);
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.of(status));

        assertThatThrownBy(() -> materialService.completeMaterial("jwt", new MaterialCompletionRequest("mat-1")))
                .isInstanceOf(ForbiddenException.class);
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
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(materialRepository.insertXpEarning("mat-1", "access-1", (short) 10)).thenReturn(1);
        MaterialCompletionProjection completion = mock(MaterialCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(completion);

        MaterialCompletionResponse response = materialService.completeMaterial("jwt",
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
        level.setLevelNumber((short) 0);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(materialRepository.insertXpEarning("mat-1", "access-1", (short) 10)).thenReturn(0);
        MaterialCompletionProjection completion = mock(MaterialCompletionProjection.class);
        when(completion.getCompletedAt()).thenReturn(Instant.now());
        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(completion);

        MaterialCompletionResponse response = materialService.completeMaterial("jwt",
                new MaterialCompletionRequest("mat-1"));

        assertThat(response.completed()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo((short) 0);
    }
}
