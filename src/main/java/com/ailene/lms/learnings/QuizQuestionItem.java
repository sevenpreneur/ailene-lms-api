package com.ailene.lms.learnings;

import java.util.List;

public record QuizQuestionItem(Integer id, String question, Short orderIndex, Short xpReward,
        List<QuizOptionItem> options) {
}
