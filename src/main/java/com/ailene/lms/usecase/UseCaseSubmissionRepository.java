package com.ailene.lms.usecase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UseCaseSubmissionRepository extends JpaRepository<UseCaseSubmission, Integer> {

    Optional<UseCaseSubmission> findByStudentAccessIdAndUseCaseId(String studentAccessId, Integer useCaseId);
}
