package com.ailene.lms.learnings;

import com.ailene.lms.chaptertest.ChapterTestTaskItem;
import com.ailene.lms.survey.ChapterSurveyStatus;

import java.util.List;

public record LearningsResponse(List<QuizTaskItem> quizzes, List<VideoTaskItem> videos,
        List<MaterialTaskItem> materials, ChapterTestTaskItem preTest, ChapterTestTaskItem postTest,
        ChapterSurveyStatus survey) {
}
