package com.ailene.lms.learnings;

import java.util.List;

public record LearningsResponse(List<QuizTaskItem> quizzes, List<VideoTaskItem> videos,
        List<MaterialTaskItem> materials) {
}
