package com.ailene.lms.survey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChapterSurveyRepository extends JpaRepository<ChapterSurvey, Integer> {

    Optional<ChapterSurvey> findByStudentAccessIdAndChapterId(String studentAccessId, Integer chapterId);
}
