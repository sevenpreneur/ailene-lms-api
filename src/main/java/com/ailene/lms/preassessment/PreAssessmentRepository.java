package com.ailene.lms.preassessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreAssessmentRepository extends JpaRepository<PreAssessment, Integer> {

    Optional<PreAssessment> findByAccessId(String accessId);
}
