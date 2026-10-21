package com.ailene.lms.admin;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.preassessment.PreAssessmentBaselineReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInsightsServiceTest {

    private static final String PROJECT = "proj-1";
    private static final AdminInsightsChapter CHAPTER = new AdminInsightsChapter(7, "Prompting 101", 1);

    @Mock
    private AdminGuard guard;
    @Mock
    private AdminCurriculumRepository curriculumRepository;
    @Mock
    private AdminInsightsRepository repository;
    @Mock
    private PreAssessmentBaselineReader baselineReader;

    @InjectMocks
    private AdminInsightsService service;

    private static AdminInsightsLearner learner(String id) {
        return new AdminInsightsLearner(id, "student", "Name " + id, id + "@x.id", null, 1, "Finance");
    }

    private static AdminQuizInsights.Submission quiz(int id, String access, int attempt, int score) {
        return new AdminQuizInsights.Submission(id, learner(access), "q1", "Quiz", CHAPTER, attempt, score, null,
                null);
    }

    @Test
    void unknownGroupIsRejectedBeforeAnyRead() {
        when(curriculumRepository.groupExists(PROJECT, 99)).thenReturn(false);

        assertThatThrownBy(() -> service.quizzes("t", new AdminInsightsRequest(PROJECT, 99)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Group not found");
        verifyNoInteractions(repository);
    }

    @Test
    void quizAveragesUseEachLearnersBestAttempt() {
        when(repository.quizSubmissions(PROJECT, null)).thenReturn(List.of(quiz(1, "a", 1, 40), quiz(2, "a", 2, 100),
                quiz(3, "b", 1, 60)));
        when(repository.quizzes(PROJECT)).thenReturn(List.of(new AdminQuizInsights.QuizRow("q1", "Quiz", CHAPTER, 5,
                0, 0, null, null, null)));

        AdminQuizInsights result = service.quizzes("t", new AdminInsightsRequest(PROJECT, null));

        assertThat(result.summary().submissions()).isEqualTo(3);
        assertThat(result.summary().participants()).isEqualTo(2);
        assertThat(result.summary().averageScore()).isEqualTo(66.7);
        assertThat(result.summary().averageBestScore()).isEqualTo(80.0);
        assertThat(result.summary().perfectScores()).isEqualTo(1);
        assertThat(result.distribution()).extracting(AdminQuizInsights.Bucket::count).containsExactly(0, 1, 0, 1);
        assertThat(result.quizzes().getFirst().attempts()).isEqualTo(3);
        assertThat(result.quizzes().getFirst().highestScore()).isEqualTo(100);
    }

    @Test
    void upliftOnlyCountsLearnersWithBothTests() {
        when(repository.testResults(PROJECT, null)).thenReturn(List.of(
                new AdminTestInsights.Row(learner("a"), CHAPTER, true, 40, null, true, 80, null, 40),
                new AdminTestInsights.Row(learner("b"), CHAPTER, true, 60, null, false, null, null, null)));
        when(repository.chapterTestKinds(PROJECT)).thenReturn(Map.of(7,
                new AdminInsightsRepository.TestKinds(true, true)));
        when(repository.chapters(PROJECT)).thenReturn(List.of(CHAPTER, new AdminInsightsChapter(8, "No tests", 1)));

        AdminTestInsights result = service.chapterTests("t", new AdminInsightsRequest(PROJECT, null));

        assertThat(result.summary().preAverage()).isEqualTo(50.0);
        assertThat(result.summary().postAverage()).isEqualTo(80.0);
        assertThat(result.summary().averageUplift()).isEqualTo(40.0);
        assertThat(result.summary().pairedCount()).isEqualTo(1);
        assertThat(result.chapters()).hasSize(1);
        assertThat(result.chapters().getFirst().preSubmissions()).isEqualTo(2);
    }
}
