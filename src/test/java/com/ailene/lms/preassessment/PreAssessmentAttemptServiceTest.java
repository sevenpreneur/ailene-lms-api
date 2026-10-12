package com.ailene.lms.preassessment;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreAssessmentAttemptServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PROJECT_ID = "proj-1";
    private static final String ACCESS_ID = "access-1";
    private static final String C1_CONFIG = "{\"max_score\": 12, \"level_thresholds\": {\"1\": 6, \"2\": 9}, "
            + "\"criteria\": [{\"key\": \"goal\", \"name\": \"Tujuan\", \"scale\": [\"a\", \"b\", \"c\"]}]}";

    @Mock
    private AccessRepository accessRepository;
    @Mock
    private PreAssessmentAttemptRepository attemptRepository;
    @Mock
    private PreAssessmentAnswerGrader answerGrader;
    @Mock
    private PreAssessmentScoreWriter scoreWriter;

    private PreAssessmentAttemptService service;

    private record VersionRow(Integer getId, String getCode, Integer getVersion, String getName,
            Integer getDurationMinutes) implements PreAssessmentVersionProjection {
    }

    private record ItemRow(Integer getId, String getItemCode, String getSection, String getType, String getQuestion,
            Integer getDomainId, String getDimension, Integer getCfteLevel, Boolean getIsCritical,
            Boolean getIsBehaviourGate, Integer getOverclaimItemId, String getGradingConfig)
            implements PreAssessmentItemProjection {
    }

    private record OptionRow(Integer getId, Integer getItemId, String getText, BigDecimal getCredit,
            Boolean getIsDontKnow, String getFlag) implements PreAssessmentOptionProjection {
    }

    private record AnswerRow(Integer getItemId, String getTextValue, BigDecimal getCredit, Integer getRubricTotal,
            Boolean getHasSensitiveData, String getAiFeedback, Boolean getIsGraded)
            implements PreAssessmentAnswerProjection {
    }

    private record ConfigRow(Integer getId, Integer getSecurityGateMaxBand, Integer getBehaviourGateFromBand,
            Integer getBehaviourGateMaxBand, BigDecimal getBehaviourGateMinScore, BigDecimal getBehaviourGateMinCredit,
            BigDecimal getOverclaimMinCredit, BigDecimal getOverclaimCappedCredit, Integer getTargetBand)
            implements PreAssessmentConfigProjection {
    }

    private record BandRow(Integer getBand, String getName, Integer getLevel, BigDecimal getMinP1,
            BigDecimal getMinP2, BigDecimal getMinP3) implements PreAssessmentBandProjection {
    }

    @BeforeEach
    void setUp() {
        service = new PreAssessmentAttemptService(accessRepository, attemptRepository, answerGrader, scoreWriter,
                JsonMapper.builder().build());

        Access access = new Access();
        access.setId(ACCESS_ID);
        access.setProjectId(PROJECT_ID);
        when(accessRepository.findByUserIdAndProjectId(USER_ID, PROJECT_ID)).thenReturn(Optional.of(access));

        VersionRow version = new VersionRow(1, "A", 1, "Form A", 30);
        when(attemptRepository.findProjectVersion(PROJECT_ID)).thenReturn(Optional.of(version));
        when(attemptRepository.findByAccessId(ACCESS_ID)).thenReturn(Optional.empty());
        when(attemptRepository.findItems(1)).thenReturn(List.of(
                new ItemRow(1, "A2", "profile", "single_choice", "Tools kantor?", null, null, null, false, false, null,
                        null),
                new ItemRow(2, "F1", "scenario", "single_choice", "Kenapa AI salah?", 1, "knowledge", 1, false, false,
                        null, null),
                new ItemRow(3, "C1", "practice", "text", "Tulis prompt", 3, "skills", null, false, false, null,
                        C1_CONFIG)));
        when(attemptRepository.findOptions(1)).thenReturn(List.of(
                new OptionRow(10, 1, "Akun pribadi", null, false, "shadow_ai"),
                new OptionRow(11, 1, "Hanya tools kantor", null, false, null),
                new OptionRow(20, 2, "Kunci", new BigDecimal("1.00"), false, null),
                new OptionRow(21, 2, "Distraktor 1", new BigDecimal("0.00"), false, null),
                new OptionRow(22, 2, "Distraktor 2", new BigDecimal("0.00"), false, null),
                new OptionRow(23, 2, "Distraktor 3", new BigDecimal("0.00"), false, null),
                new OptionRow(24, 2, "Saya tidak tahu", new BigDecimal("0.00"), true, null)));
        when(attemptRepository.save(any())).thenAnswer(inv -> {
            PreAssessmentAttempt attempt = inv.getArgument(0);
            attempt.setId(42);
            return attempt;
        });
    }

    private static PreAssessmentSubmitRequest request(PreAssessmentSubmitRequest.Answer... answers) {
        return new PreAssessmentSubmitRequest(PROJECT_ID, List.of(answers));
    }

    private static PreAssessmentAttempt attempt(PreAssessmentAttemptStatus status) {
        PreAssessmentAttempt attempt = new PreAssessmentAttempt();
        attempt.setId(42);
        attempt.setAccessId(ACCESS_ID);
        attempt.setVersionId(1);
        attempt.setStatus(status);
        return attempt;
    }

    @Test
    void submit_storesAnswersWithCreditAndFlagsShadowAi() {
        PreAssessmentSubmitResponse response = service.submit(USER_ID, request(
                new PreAssessmentSubmitRequest.Answer(1, 10, null),
                new PreAssessmentSubmitRequest.Answer(2, 20, null),
                new PreAssessmentSubmitRequest.Answer(3, null, "  Ringkas notulen rapat  ")));

        assertThat(response.attemptId()).isEqualTo(42);
        assertThat(response.status()).isEqualTo(PreAssessmentAttemptStatus.submitted);

        ArgumentCaptor<PreAssessmentAttempt> saved = ArgumentCaptor.forClass(PreAssessmentAttempt.class);
        verify(attemptRepository).save(saved.capture());
        assertThat(saved.getValue().getIsShadowAi()).isTrue();
        assertThat(saved.getValue().getSubmittedAt()).isNotNull();

        verify(attemptRepository).insertAnswer(42, 1, 1, 10, null, null);
        verify(attemptRepository).insertAnswer(42, 2, 1, 20, null, new BigDecimal("1.00"));
        verify(attemptRepository).insertAnswer(42, 3, 1, null, "Ringkas notulen rapat", null);
    }

    @Test
    void submit_missingQuestions_listsTheirCodes() {
        assertThatThrownBy(() -> service.submit(USER_ID, request(new PreAssessmentSubmitRequest.Answer(1, 11, null))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Every question must be answered, missing: F1, C1");
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submit_optionOfAnotherQuestion_isRejected() {
        assertThatThrownBy(() -> service.submit(USER_ID, request(
                new PreAssessmentSubmitRequest.Answer(1, 20, null),
                new PreAssessmentSubmitRequest.Answer(2, 20, null),
                new PreAssessmentSubmitRequest.Answer(3, null, "prompt"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Question A2 needs one of its own options");
    }

    @Test
    void submit_blankTextAnswer_isRejected() {
        assertThatThrownBy(() -> service.submit(USER_ID, request(
                new PreAssessmentSubmitRequest.Answer(1, 11, null),
                new PreAssessmentSubmitRequest.Answer(2, 21, null),
                new PreAssessmentSubmitRequest.Answer(3, null, "   "))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Question C1 needs a text answer");
    }

    @Test
    void submit_secondTime_isRejected() {
        when(attemptRepository.findByAccessId(ACCESS_ID)).thenReturn(Optional.of(new PreAssessmentAttempt()));

        assertThatThrownBy(() -> service.submit(USER_ID, request(new PreAssessmentSubmitRequest.Answer(1, 11, null))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pre-assessment already submitted.");
        verify(attemptRepository, never()).insertAnswer(anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void questions_shuffleOnlyScenarioOptions_keepingDontKnowLastAndHidingCredit() {
        PreAssessmentQuestionsResponse first = service.getQuestions(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID));
        PreAssessmentQuestionsResponse second = service.getQuestions(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID));

        assertThat(first.attempt()).isNull();
        assertThat(first.items().get(0).options()).extracting(PreAssessmentQuestionsResponse.Option::id)
                .containsExactly(10, 11);

        List<Integer> scenario = first.items().get(1).options().stream()
                .map(PreAssessmentQuestionsResponse.Option::id).toList();
        assertThat(scenario).containsExactlyInAnyOrder(20, 21, 22, 23, 24).last().isEqualTo(24);
        assertThat(second.items().get(1).options()).extracting(PreAssessmentQuestionsResponse.Option::id)
                .isEqualTo(scenario);
        assertThat(first.items().get(2).options()).isEmpty();
    }

    @Test
    void questions_projectWithoutVersion_isNotFound() {
        when(attemptRepository.findProjectVersion(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQuestions(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No pre-assessment is set for this project");
    }

    @Test
    void score_notSubmitted_returnsNull() {
        assertThat(service.getScore(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID))).isNull();
        verifyNoInteractions(answerGrader, scoreWriter);
    }

    @Test
    void score_submitted_gradesWrittenAnswersThenSavesTheBand() {
        PreAssessmentAttempt submitted = attempt(PreAssessmentAttemptStatus.submitted);
        PreAssessmentAttempt scored = attempt(PreAssessmentAttemptStatus.scored);
        scored.setConfigId(1);
        scored.setFinalBand((short) 3);
        scored.setRawBand((short) 3);
        when(attemptRepository.findByAccessId(ACCESS_ID)).thenReturn(Optional.of(submitted));
        when(attemptRepository.findById(42)).thenReturn(Optional.of(scored));
        when(attemptRepository.findAnswers(42)).thenReturn(List.of(
                new AnswerRow(1, null, null, null, false, null, false),
                new AnswerRow(2, null, new BigDecimal("1.00"), null, false, null, false),
                new AnswerRow(3, "Prompt saya", null, null, false, null, false)));
        when(answerGrader.grade(anyString(), anyList())).thenReturn(Map.of("C1",
                new PreAssessmentAnswerGrader.Grade(new BigDecimal("0.83"), "{\"goal\":2}", 10, false, "Bagus.")));
        ConfigRow config = new ConfigRow(1, 1, 3, 2, new BigDecimal("50"), new BigDecimal("0.5"),
                new BigDecimal("0.75"), new BigDecimal("0.25"), 2);
        when(attemptRepository.findConfig(null)).thenReturn(Optional.of(config));
        when(attemptRepository.findConfig(1)).thenReturn(Optional.of(config));
        when(attemptRepository.findBands(1)).thenReturn(List.of(
                new BandRow(2, "Basic Proficiency", 1, new BigDecimal("70"), null, null),
                new BandRow(3, "Working Proficiency", 2, new BigDecimal("70"), new BigDecimal("60"), null)));

        PreAssessmentScoreResponse response = service.getScore(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID));

        ArgumentCaptor<List<PreAssessmentAnswerGrader.Task>> tasks = ArgumentCaptor.forClass(List.class);
        verify(answerGrader).grade(anyString(), tasks.capture());
        assertThat(tasks.getValue()).extracting(PreAssessmentAnswerGrader.Task::itemCode).containsExactly("C1");
        verify(scoreWriter).saveGrades(eq(42), anyMap());
        verify(scoreWriter).saveResult(eq(42), eq(1), any());

        assertThat(response.status()).isEqualTo(PreAssessmentAttemptStatus.scored);
        assertThat(response.band().name()).isEqualTo("Working Proficiency");
        assertThat(response.targetBand().band()).isEqualTo(2);
        assertThat(response.gap()).isZero();
        assertThat(response.meetsTarget()).isTrue();
    }

    @Test
    void score_alreadyScored_onlyReads() {
        PreAssessmentAttempt scored = attempt(PreAssessmentAttemptStatus.scored);
        scored.setConfigId(1);
        scored.setFinalBand((short) 1);
        scored.setRawBand((short) 1);
        when(attemptRepository.findByAccessId(ACCESS_ID)).thenReturn(Optional.of(scored));
        when(attemptRepository.findConfig(1)).thenReturn(Optional.of(new ConfigRow(1, 1, 3, 2, BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 2)));

        PreAssessmentScoreResponse response = service.getScore(USER_ID, new PreAssessmentProjectRequest(PROJECT_ID));

        verifyNoInteractions(answerGrader, scoreWriter);
        assertThat(response.gap()).isEqualTo(1);
        assertThat(response.meetsTarget()).isFalse();
    }
}
