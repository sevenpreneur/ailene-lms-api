package com.ailene.lms.prompt;

import com.ailene.lms.common.qstash.QStashClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PromptEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PromptEvaluationService.class);
    private static final String EVALUATION_JOB_PATH = "/api/v1/prompts/evaluate-callback";

    private final PromptSubmissionRepository promptSubmissionRepository;
    private final PromptRepository promptRepository;
    private final PromptRubricEvaluator promptRubricEvaluator;
    private final QStashClient qStashClient;

    // A resubmission is new work, so the previous scores (AI's or champion's) no longer apply.
    public void markPending(PromptSubmission submission) {
        submission.setRubricSpecificity(null);
        submission.setRubricContext(null);
        submission.setRubricConstraints(null);
        submission.setRubricExamples(null);
        submission.setRubricIteration(null);
        submission.setAiFeedback(null);
        submission.setAiEvaluatedAt(null);
        submission.setAiStatus(PromptEvaluationStatus.pending);
    }

    public void scheduleAfterCommit(PromptSubmission submission) {
        PromptEvaluationJobRequest job = new PromptEvaluationJobRequest(submission.getId(),
                submission.getSubmittedAt().toInstant().toEpochMilli());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    qStashClient.publish(EVALUATION_JOB_PATH, job);
                }
            });
        } else {
            qStashClient.publish(EVALUATION_JOB_PATH, job);
        }
    }

    // No enclosing @Transactional: the failed status must commit even though the exception is rethrown for a retry.
    public void evaluate(PromptEvaluationJobRequest job) {
        PromptSubmission submission = promptSubmissionRepository.findById(job.submissionId()).orElse(null);
        if (submission == null || submission.getSubmittedAt() == null
                || submission.getSubmittedAt().toInstant().toEpochMilli() != job.submittedAtMillis()) {
            log.info("Skipping stale prompt evaluation job for submission {}", job.submissionId());
            return;
        }
        Prompt prompt = promptRepository.findById(submission.getPromptId()).orElse(null);
        if (prompt == null) {
            log.warn("Skipping prompt evaluation: prompt {} is gone", submission.getPromptId());
            return;
        }

        try {
            String expectedOutput = Boolean.TRUE.equals(prompt.getIsSelfCreated()) ? null : prompt.getExpectedOutput();
            PromptRubricScore score = promptRubricEvaluator.evaluate(prompt.getScenario(), expectedOutput,
                    submission.getInput(), submission.getOutput());
            // A champion who reviewed before the AI finished keeps their scores; the AI only fills the gaps.
            if (submission.getRubricSpecificity() == null) {
                submission.setRubricSpecificity(score.specificity());
            }
            if (submission.getRubricContext() == null) {
                submission.setRubricContext(score.context());
            }
            if (submission.getRubricConstraints() == null) {
                submission.setRubricConstraints(score.constraints());
            }
            if (submission.getRubricExamples() == null) {
                submission.setRubricExamples(score.examples());
            }
            if (submission.getRubricIteration() == null) {
                submission.setRubricIteration(score.iteration());
            }
            submission.setAiFeedback(score.feedback());
            submission.setAiEvaluatedAt(OffsetDateTime.now());
            submission.setAiStatus(PromptEvaluationStatus.completed);
            promptSubmissionRepository.save(submission);
        } catch (RuntimeException e) {
            submission.setAiStatus(PromptEvaluationStatus.failed);
            promptSubmissionRepository.save(submission);
            throw e;
        }
    }
}
