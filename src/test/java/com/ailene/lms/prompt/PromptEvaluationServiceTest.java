package com.ailene.lms.prompt;

import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import com.ailene.lms.common.qstash.QStashClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromptEvaluationServiceTest {

    private final PromptSubmissionRepository submissionRepository = mock(PromptSubmissionRepository.class);
    private final PromptRepository promptRepository = mock(PromptRepository.class);
    private final DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
    private final QStashClient qStashClient = mock(QStashClient.class);
    private final PromptRubricEvaluator evaluator = new PromptRubricEvaluator(deepSeekClient, new ObjectMapper());
    private final PromptEvaluationService service = new PromptEvaluationService(submissionRepository,
            promptRepository, evaluator, qStashClient);

    private final OffsetDateTime submittedAt = OffsetDateTime.parse("2026-09-25T05:00:00.123456789Z");

    @Test
    void evaluate_writesAiScoresAndFeedback() {
        PromptSubmission submission = submission();
        when(deepSeekClient.createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn("""
                {"reasoning":"r","specificity":4,"context":"3","constraints":2.4,"examples":1,"iteration":5,
                 "feedback":" Bagus. "}""");

        service.evaluate(job());

        assertThat(submission.getAiStatus()).isEqualTo(PromptEvaluationStatus.completed);
        assertThat(submission.getRubricSpecificity()).isEqualTo((short) 4);
        assertThat(submission.getRubricContext()).isEqualTo((short) 3);
        assertThat(submission.getRubricConstraints()).isEqualTo((short) 2);
        assertThat(submission.getRubricIteration()).isEqualTo((short) 5);
        assertThat(submission.getAiFeedback()).isEqualTo("Bagus.");
        assertThat(submission.getAiEvaluatedAt()).isNotNull();
    }

    @Test
    void evaluate_championScoredFirst_keepsChampionScores() {
        PromptSubmission submission = submission();
        submission.setRubricContext((short) 5);
        when(deepSeekClient.createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn("""
                {"specificity":2,"context":1,"constraints":2,"examples":1,"iteration":2,"feedback":"f"}""");

        service.evaluate(job());

        assertThat(submission.getRubricContext()).isEqualTo((short) 5);
        assertThat(submission.getRubricSpecificity()).isEqualTo((short) 2);
        assertThat(submission.getAiStatus()).isEqualTo(PromptEvaluationStatus.completed);
    }

    @Test
    void evaluate_selfCreatedPrompt_hidesPlaceholderTarget() {
        PromptSubmission submission = submission();
        Prompt prompt = promptRepository.findById(submission.getPromptId()).orElseThrow();
        prompt.setIsSelfCreated(true);
        when(deepSeekClient.createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn("""
                {"specificity":1,"context":1,"constraints":1,"examples":1,"iteration":1,"feedback":"f"}""");

        service.evaluate(job());

        verify(deepSeekClient).createJsonCompletion(anyString(),
                contains("Target output: (tidak ada)"), anyInt(), anyDouble());
    }

    @Test
    void evaluate_outOfRangeScore_marksFailedAndRethrowsForRetry() {
        PromptSubmission submission = submission();
        when(deepSeekClient.createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn("""
                {"specificity":7,"context":3,"constraints":2,"examples":1,"iteration":5,"feedback":"f"}""");

        assertThatThrownBy(() -> service.evaluate(job())).isInstanceOf(BadGatewayException.class);
        assertThat(submission.getAiStatus()).isEqualTo(PromptEvaluationStatus.failed);
        assertThat(submission.getRubricSpecificity()).isNull();
    }

    @Test
    void evaluate_resubmittedSinceScheduling_skipsStaleJob() {
        PromptSubmission submission = submission();
        submission.setSubmittedAt(submittedAt.plusMinutes(1));

        service.evaluate(job());

        verify(deepSeekClient, never()).createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void markPending_clearsPreviousScores() {
        PromptSubmission submission = new PromptSubmission();
        submission.setRubricSpecificity((short) 5);
        submission.setAiFeedback("old");
        submission.setAiStatus(PromptEvaluationStatus.completed);

        service.markPending(submission);

        assertThat(submission.getRubricSpecificity()).isNull();
        assertThat(submission.getAiFeedback()).isNull();
        assertThat(submission.getAiStatus()).isEqualTo(PromptEvaluationStatus.pending);
    }

    @Test
    void scheduleAfterCommit_outsideTransaction_publishesImmediately() {
        PromptSubmission submission = new PromptSubmission();
        submission.setId(7);
        submission.setSubmittedAt(submittedAt);

        service.scheduleAfterCommit(submission);

        verify(qStashClient).publish(eq("/api/v1/prompts/evaluate-callback"),
                eq(new PromptEvaluationJobRequest(7, submittedAt.toInstant().toEpochMilli())));
    }

    private PromptEvaluationJobRequest job() {
        return new PromptEvaluationJobRequest(7, submittedAt.toInstant().toEpochMilli());
    }

    private PromptSubmission submission() {
        PromptSubmission submission = new PromptSubmission();
        submission.setId(7);
        submission.setPromptId(3);
        submission.setInput("Buatkan job description");
        submission.setOutput("JD ...");
        submission.setSubmittedAt(submittedAt.withNano(123456000));
        submission.setAiStatus(PromptEvaluationStatus.pending);
        Prompt prompt = new Prompt();
        prompt.setId(3);
        prompt.setScenario("Anda adalah HR");
        prompt.setExpectedOutput("JD lengkap");
        prompt.setIsSelfCreated(false);
        when(submissionRepository.findById(7)).thenReturn(Optional.of(submission));
        when(promptRepository.findById(3)).thenReturn(Optional.of(prompt));
        return submission;
    }
}
