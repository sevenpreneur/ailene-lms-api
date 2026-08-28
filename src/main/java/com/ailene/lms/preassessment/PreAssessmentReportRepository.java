package com.ailene.lms.preassessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreAssessmentReportRepository extends JpaRepository<PreAssessmentReport, Integer> {

    Optional<PreAssessmentReport> findByPreAssessmentId(Integer preAssessmentId);
}
