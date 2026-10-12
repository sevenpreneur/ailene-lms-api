package com.ailene.lms.preassessment;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

// Persists grading and scoring in their own transactions, so the DeepSeek call before them holds no connection.
@Component
@RequiredArgsConstructor
public class PreAssessmentScoreWriter {

    private final PreAssessmentAttemptRepository attemptRepository;

    @Transactional
    public void saveGrades(Integer attemptId, Map<Integer, PreAssessmentAnswerGrader.Grade> gradesByItemId) {
        gradesByItemId.forEach((itemId, grade) -> attemptRepository.updateGrade(attemptId, itemId, grade.credit(),
                grade.rubricScores(), grade.rubricTotal(), grade.sensitiveData(), grade.feedback()));
    }

    // The row lock makes a concurrent score call wait, then find the attempt already scored and skip.
    @Transactional
    public void saveResult(Integer attemptId, Integer configId, PreAssessmentBandEngine.Result result) {
        PreAssessmentAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment attempt not found"));
        if (attempt.getStatus() == PreAssessmentAttemptStatus.scored) {
            return;
        }

        attempt.setConfigId(configId);
        attempt.setP1(result.p1());
        attempt.setP2(result.p2());
        attempt.setP3(result.p3());
        attempt.setKnowledgeScore(result.knowledge());
        attempt.setSkillsScore(result.skills());
        attempt.setBehavioursScore(result.behaviours());
        attempt.setRawBand((short) result.rawBand());
        attempt.setFinalBand((short) result.finalBand());
        attempt.setLevel((short) result.level());
        attempt.setSecurityGatePassed(result.securityGatePassed());
        attempt.setBehaviourGatePassed(result.behaviourGatePassed());
        attempt.setIsOverclaim(result.overclaim());
        attempt.setStatus(PreAssessmentAttemptStatus.scored);
        attempt.setScoredAt(OffsetDateTime.now());
        attemptRepository.save(attempt);

        result.domainScores().forEach(d ->
                attemptRepository.insertDomainScore(attemptId, d.domainId(), d.score(), d.itemCount()));
    }
}
