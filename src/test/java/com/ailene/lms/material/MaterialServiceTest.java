package com.ailene.lms.material;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterLockedReason;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

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
    @Mock
    private ChapterAccessGuard chapterAccessGuard;

    @InjectMocks
    private MaterialService materialService;

    @BeforeEach
    void setUp() {
        when(authService.resolveUserId("jwt")).thenReturn(USER_ID);

        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setLevelId(1);
        chapter.setName("Chapter");
        chapter.setProjectId("proj-1");
        lenient().when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        Access access = new Access();
        access.setId("access-1");
        access.setProjectId("proj-1");
        lenient().when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
    }

    private Material givenMaterial(String id) {
        Material material = new Material();
        material.setId(id);
        material.setChapterId(1);
        material.setTitle("Title");
        material.setXpReward((short) 10);
        material.setOrderIndex((short) 0);
        when(materialRepository.findById(id)).thenReturn(Optional.of(material));
        return material;
    }

    private void givenChapterLocked(ChapterLockedReason reason) {
        when(chapterAccessGuard.requireUnlocked(1, "access-1"))
                .thenThrow(new ForbiddenException(ChapterAccessGuard.message(reason)));
    }

    private static LevelMaterialProjection levelRow(String id, Instant sessionDate, Boolean sessionLocked,
            boolean completed) {
        LevelMaterialProjection row = mock(LevelMaterialProjection.class);
        when(row.getMaterialId()).thenReturn(id);
        when(row.getMaterialTitle()).thenReturn(id.toUpperCase());
        when(row.getSessionDate()).thenReturn(sessionDate);
        lenient().when(row.getSessionLocked()).thenReturn(sessionLocked);
        when(row.getCompleted()).thenReturn(completed);
        return row;
    }

    // findCompletion() returns null (not a projection with a null getter) when the student has no completion row.
    @Test
    void materialDetails_uncompletedMaterial_doesNotThrow() {
        givenMaterial("mat-1");
        when(materialRepository.findCompletion("mat-1", "access-1")).thenReturn(null);

        MaterialDetailsResponse response = materialService.getMaterialDetails("jwt",
                new MaterialDetailsRequest("mat-1"));

        assertThat(response.completed()).isFalse();
        assertThat(response.completedAt()).isNull();
        verify(chapterAccessGuard).requireUnlocked(1, "access-1");
    }

    @Test
    void materialDetails_sessionNotStarted_throwsForbidden() {
        givenMaterial("mat-1");
        givenChapterLocked(ChapterLockedReason.session_not_started);

        assertThatThrownBy(() -> materialService.getMaterialDetails("jwt", new MaterialDetailsRequest("mat-1")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter opens when its session starts.");
    }

    @Test
    void completeMaterial_adminLocked_throwsForbiddenAndWritesNothing() {
        givenMaterial("mat-1");
        givenChapterLocked(ChapterLockedReason.admin_locked);

        assertThatThrownBy(() -> materialService.completeMaterial("jwt", new MaterialCompletionRequest("mat-1")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This chapter has been locked by an admin.");
        verify(materialRepository, never()).insertCompletion(anyString(), anyString());
        verify(materialRepository, never()).insertXpEarning(anyString(), anyString(), any());
    }

    @Test
    void completeMaterial_firstCompletion_awardsXp() {
        givenMaterial("mat-1");
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
        givenMaterial("mat-1");
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
    void levelMaterials_locksUnstartedAndAdminLockedSessionsOnly() {
        givenMaterial("mat-1");
        Level level = new Level();
        level.setId(1);
        level.setLevelNumber((short) 2);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        LevelMaterialProjection open = levelRow("mat-1", yesterday, false, true);
        LevelMaterialProjection future = levelRow("mat-2", Instant.now().plus(1, ChronoUnit.DAYS), false, false);
        LevelMaterialProjection adminLocked = levelRow("mat-3", yesterday, true, false);
        when(materialRepository.findLevelMaterials(1, "access-1")).thenReturn(List.of(open, future, adminLocked));

        LevelMaterialsResponse response = materialService.getLevelMaterials("jwt",
                new LevelMaterialsRequest("mat-1"));

        assertThat(response.levelNumber()).isEqualTo((short) 2);
        assertThat(response.materials()).extracting(LevelMaterialItem::id)
                .containsExactly("mat-1", "mat-2", "mat-3");
        assertThat(response.materials()).extracting(LevelMaterialItem::index).containsExactly(1, 2, 3);
        assertThat(response.materials()).extracting(LevelMaterialItem::locked).containsExactly(false, true, true);
        assertThat(response.materials()).extracting(LevelMaterialItem::isCurrent).containsExactly(true, false, false);
        assertThat(response.materials().get(0).completed()).isTrue();
    }

    @Test
    void levelMaterials_lockedAnchorChapter_throwsForbidden() {
        givenMaterial("mat-1");
        givenChapterLocked(ChapterLockedReason.admin_locked);

        assertThatThrownBy(() -> materialService.getLevelMaterials("jwt", new LevelMaterialsRequest("mat-1")))
                .isInstanceOf(ForbiddenException.class);
        verify(materialRepository, never()).findLevelMaterials(any(), anyString());
    }
}
