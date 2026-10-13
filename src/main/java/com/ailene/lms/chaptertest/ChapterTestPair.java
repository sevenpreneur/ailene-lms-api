package com.ailene.lms.chaptertest;

import java.util.List;

public record ChapterTestPair(ChapterTestTaskProjection preTest, ChapterTestTaskProjection postTest) {

    public static ChapterTestPair from(List<ChapterTestTaskProjection> rows) {
        ChapterTestTaskProjection pre = null;
        ChapterTestTaskProjection post = null;
        for (ChapterTestTaskProjection row : rows) {
            if (ChapterTestKind.pre_test.name().equals(row.getKind())) {
                pre = row;
            } else if (ChapterTestKind.post_test.name().equals(row.getKind())) {
                post = row;
            }
        }
        return new ChapterTestPair(pre, post);
    }

    public ChapterTestTaskProjection get(ChapterTestKind kind) {
        return kind == ChapterTestKind.pre_test ? preTest : postTest;
    }

    // The pre-test must come before any learning it measures, and the post-test after all of it.
    public ChapterTestLockedReason lockedReason(ChapterTestKind kind, long incompleteMaterials) {
        if (submitted(get(kind))) {
            return null;
        }
        if (kind == ChapterTestKind.pre_test) {
            return submitted(postTest) ? ChapterTestLockedReason.post_test_submitted : null;
        }
        if (preTest != null && !submitted(preTest)) {
            return ChapterTestLockedReason.pre_test_required;
        }
        return incompleteMaterials > 0 ? ChapterTestLockedReason.materials_incomplete : null;
    }

    // Pre-test answers stay hidden while a post-test that may reuse its questions is still ahead.
    public boolean revealsAnswers() {
        return postTest == null || submitted(postTest);
    }

    public static boolean submitted(ChapterTestTaskProjection test) {
        return test != null && test.getSubmittedAt() != null;
    }
}
