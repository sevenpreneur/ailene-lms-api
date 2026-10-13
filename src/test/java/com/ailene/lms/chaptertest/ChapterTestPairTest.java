package com.ailene.lms.chaptertest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class ChapterTestPairTest {

    private static ChapterTestTaskProjection row(ChapterTestKind kind, boolean submitted) {
        ChapterTestTaskProjection row = mock(ChapterTestTaskProjection.class);
        lenient().when(row.getKind()).thenReturn(kind.name());
        lenient().when(row.getSubmittedAt()).thenReturn(submitted ? Instant.now() : null);
        return row;
    }

    @Test
    void postTestWaitsForPreTestThenMaterials() {
        var pair = ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, false),
                row(ChapterTestKind.post_test, false)));

        assertThat(pair.lockedReason(ChapterTestKind.pre_test, 3)).isNull();
        assertThat(pair.lockedReason(ChapterTestKind.post_test, 0))
                .isEqualTo(ChapterTestLockedReason.pre_test_required);

        var afterPre = ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, true),
                row(ChapterTestKind.post_test, false)));
        assertThat(afterPre.lockedReason(ChapterTestKind.post_test, 2))
                .isEqualTo(ChapterTestLockedReason.materials_incomplete);
        assertThat(afterPre.lockedReason(ChapterTestKind.post_test, 0)).isNull();
    }

    @Test
    void postTestWithoutPreTestOnlyNeedsMaterials() {
        var pair = ChapterTestPair.from(List.of(row(ChapterTestKind.post_test, false)));

        assertThat(pair.lockedReason(ChapterTestKind.post_test, 0)).isNull();
    }

    @Test
    void preTestClosesOnceThePostTestIsIn() {
        var pair = ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, false),
                row(ChapterTestKind.post_test, true)));

        assertThat(pair.lockedReason(ChapterTestKind.pre_test, 0))
                .isEqualTo(ChapterTestLockedReason.post_test_submitted);
    }

    @Test
    void preTestAnswersStayHiddenUntilThePostTestIsSubmitted() {
        assertThat(ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, true),
                row(ChapterTestKind.post_test, false))).revealsAnswers()).isFalse();
        assertThat(ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, true),
                row(ChapterTestKind.post_test, true))).revealsAnswers()).isTrue();
        assertThat(ChapterTestPair.from(List.of(row(ChapterTestKind.pre_test, true))).revealsAnswers()).isTrue();
    }
}
