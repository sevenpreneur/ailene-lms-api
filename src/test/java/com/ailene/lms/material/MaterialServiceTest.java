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
import java.time.temporal.ChronoUnit;
import java.util.List;
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

        LevelMaterialsResponse response = materialService.getLevelMaterials("jwt",
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

        LevelMaterialsResponse response = materialService.getLevelMaterials("jwt",
                new LevelMaterialsRequest("mat-1"));

        assertThat(response.materials()).hasSize(1);
        assertThat(response.materials().get(0).locked()).isTrue();
    }
}
