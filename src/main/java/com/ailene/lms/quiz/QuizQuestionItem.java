package com.ailene.lms.quiz;

import java.util.List;

public record QuizQuestionItem(Integer id, String question, Short orderIndex, Short xpReward,
        List<QuizOptionItem> options) {
}
