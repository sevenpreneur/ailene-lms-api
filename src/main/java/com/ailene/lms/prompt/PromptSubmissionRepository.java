package com.ailene.lms.prompt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptSubmissionRepository extends JpaRepository<PromptSubmission, Integer> {

    Optional<PromptSubmission> findByStudentAccessIdAndPromptId(String studentAccessId, Integer promptId);
}
