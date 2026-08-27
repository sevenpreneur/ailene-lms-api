package com.ailene.lms.quiz;

public interface QuizFocusProjection {
    Integer getChapterId();

    String getQuizId();

    String getName();

    Short getOrderIndex();

    Boolean getCompleted();
}
