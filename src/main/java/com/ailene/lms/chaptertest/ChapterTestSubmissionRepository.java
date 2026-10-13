package com.ailene.lms.chaptertest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChapterTestSubmissionRepository extends JpaRepository<ChapterTestSubmission, Integer> {

    Optional<ChapterTestSubmission> findByStudentAccessIdAndTestId(String studentAccessId, Integer testId);
}
