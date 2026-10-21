package com.ailene.lms.admin;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.preassessment.PreAssessmentBaseline;
import com.ailene.lms.preassessment.PreAssessmentBaselineReader;
import com.ailene.lms.usecase.GuidedStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ailene.lms.admin.AdminInsightsMath.average;

@Service
@RequiredArgsConstructor
public class AdminInsightsService {

    private static final List<AdminQuizInsights.Bucket> BUCKETS = List.of(
            new AdminQuizInsights.Bucket("0-49", 0, 49, 0), new AdminQuizInsights.Bucket("50-69", 50, 69, 0),
            new AdminQuizInsights.Bucket("70-84", 70, 84, 0), new AdminQuizInsights.Bucket("85-100", 85, 100, 0));

    private final AdminGuard guard;
    private final AdminCurriculumRepository curriculumRepository;
    private final AdminInsightsRepository repository;
    private final PreAssessmentBaselineReader baselineReader;

    @Transactional(readOnly = true)
    public AdminInsightsOverview overview(String token, AdminInsightsRequest r) {
        authorize(token, r);
        AdminPreAssessmentInsights.Summary preAssessment;
        try {
            preAssessment = buildPreAssessment(r).summary();
        } catch (ResourceNotFoundException e) {
            preAssessment = null;
        }
        return new AdminInsightsOverview(repository.countLearners(r.projectId(), r.groupId()), preAssessment,
                buildGuided(r).summary(), buildReviews(r).summary(), buildTests(r).summary(),
                buildQuizzes(r).summary());
    }

    @Transactional(readOnly = true)
    public AdminPreAssessmentInsights preAssessment(String token, AdminInsightsRequest r) {
        authorize(token, r);
        return buildPreAssessment(r);
    }

    @Transactional(readOnly = true)
    public AdminGuidedInsights guidedUseCases(String token, AdminInsightsRequest r) {
        authorize(token, r);
        return buildGuided(r);
    }

    @Transactional(readOnly = true)
    public AdminReviewInsights sessionReviews(String token, AdminInsightsRequest r) {
        authorize(token, r);
        return buildReviews(r);
    }

    @Transactional(readOnly = true)
    public AdminTestInsights chapterTests(String token, AdminInsightsRequest r) {
        authorize(token, r);
        return buildTests(r);
    }

    @Transactional(readOnly = true)
    public AdminQuizInsights quizzes(String token, AdminInsightsRequest r) {
        authorize(token, r);
        return buildQuizzes(r);
    }

    private void authorize(String token, AdminInsightsRequest r) {
        guard.requireAdminOnProject(token, r.projectId());
        if (r.groupId() != null && !curriculumRepository.groupExists(r.projectId(), r.groupId())) {
            throw new ResourceNotFoundException("Group not found");
        }
    }

    private AdminPreAssessmentInsights buildPreAssessment(AdminInsightsRequest r) {
        PreAssessmentBaseline baseline = baselineReader.read(r.projectId(), r.groupId());
        var flags = repository.preAssessmentFlags(r.projectId(), r.groupId());
        List<PreAssessmentBaseline.Member> members = baseline.members();

        var rows = members.stream().map(m -> new AdminPreAssessmentInsights.Member(m.accessId(), m.fullName(),
                m.avatar(), m.groupId(), m.groupName(), m.status(), baseline.band(m.band()), m.average(),
                baseline.meetsTarget(m), m.submittedAt(), baseline.memberClusters(m), flags.get(m.accessId())))
                .toList();

        var groups = groupBy(members, PreAssessmentBaseline.Member::groupId).values().stream()
                .map(g -> new AdminPreAssessmentInsights.GroupRow(g.getFirst().groupId(), g.getFirst().groupName(),
                        g.size(), (int) g.stream().filter(PreAssessmentBaseline.Member::scored).count(),
                        baseline.meetsTargetCount(g), PreAssessmentBaseline.average(g)))
                .toList();

        var summary = new AdminPreAssessmentInsights.Summary(members.size(),
                (int) members.stream().filter(PreAssessmentBaseline.Member::submitted).count(),
                (int) members.stream().filter(PreAssessmentBaseline.Member::scored).count(),
                baseline.meetsTargetCount(members), PreAssessmentBaseline.average(members),
                countFlags(flags.values(), f -> Boolean.FALSE.equals(f.securityGatePassed())),
                countFlags(flags.values(), f -> Boolean.TRUE.equals(f.isOverclaim())),
                countFlags(flags.values(), f -> Boolean.TRUE.equals(f.isShadowAi())));
        return new AdminPreAssessmentInsights(summary, baseline.targetBand(), baseline.bandCounts(members),
                baseline.averageClusters(members), groups, rows);
    }

    private AdminGuidedInsights buildGuided(AdminInsightsRequest r) {
        var rows = repository.guidedSessions(r.projectId(), r.groupId());
        var funnel = Arrays.stream(GuidedStatus.values())
                .map(s -> new AdminGuidedInsights.StatusCount(s.name(),
                        (int) rows.stream().filter(row -> s.name().equals(row.status())).count()))
                .toList();
        var summary = new AdminGuidedInsights.Summary(rows.size(),
                countReview(rows, AdminGuidedInsights.ReviewStatus.in_progress),
                countReview(rows, AdminGuidedInsights.ReviewStatus.awaiting_review),
                countReview(rows, AdminGuidedInsights.ReviewStatus.needs_revision),
                countReview(rows, AdminGuidedInsights.ReviewStatus.accepted),
                average(rows.stream().filter(row -> row.minutesWithoutAi() != null
                        && row.estimatedMinutesWithAi() != null)
                        .map(row -> row.minutesWithoutAi() - row.estimatedMinutesWithAi())),
                AdminInsightsMath.sum(rows.stream().map(AdminGuidedInsights.Row::hoursSaved)),
                average(rows.stream().map(AdminGuidedInsights.Row::helpfulnessRating)));
        return new AdminGuidedInsights(summary, funnel, rows);
    }

    private AdminReviewInsights buildReviews(AdminInsightsRequest r) {
        var responses = repository.surveys(r.projectId(), r.groupId());
        var byChapter = groupBy(responses, response -> response.chapter().id());
        var chapters = repository.chapters(r.projectId()).stream().map(chapter -> {
            var list = byChapter.getOrDefault(chapter.id(), List.of());
            var ratings = averageRatings(list);
            return new AdminReviewInsights.ChapterRow(chapter, list.size(), ratings,
                    list.isEmpty() ? null : AdminInsightsMath.trainer(ratings),
                    list.isEmpty() ? null : AdminInsightsMath.material(ratings),
                    average(list.stream().map(AdminReviewInsights.Response::overallAverage)));
        }).toList();
        var all = averageRatings(responses);
        var summary = new AdminReviewInsights.Summary(repository.countLearners(r.projectId(), r.groupId()),
                responses.size(), responses.isEmpty() ? null : AdminInsightsMath.trainer(all),
                responses.isEmpty() ? null : AdminInsightsMath.material(all), all.learningValue(),
                average(responses.stream().map(AdminReviewInsights.Response::overallAverage)));
        return new AdminReviewInsights(summary, chapters, responses);
    }

    private AdminTestInsights buildTests(AdminInsightsRequest r) {
        var results = repository.testResults(r.projectId(), r.groupId());
        var kinds = repository.chapterTestKinds(r.projectId());
        var byChapter = groupBy(results, row -> row.chapter().id());
        var chapters = repository.chapters(r.projectId()).stream()
                .filter(c -> kinds.containsKey(c.id()) || byChapter.containsKey(c.id()))
                .map(c -> {
                    var list = byChapter.getOrDefault(c.id(), List.of());
                    var kind = kinds.getOrDefault(c.id(), new AdminInsightsRepository.TestKinds(false, false));
                    return new AdminTestInsights.ChapterRow(c, kind.hasPre(), kind.hasPost(),
                            (int) list.stream().filter(AdminTestInsights.Row::preSubmitted).count(),
                            (int) list.stream().filter(AdminTestInsights.Row::postSubmitted).count(),
                            average(list.stream().map(AdminTestInsights.Row::preScore)),
                            average(list.stream().map(AdminTestInsights.Row::postScore)),
                            average(list.stream().map(AdminTestInsights.Row::uplift)));
                }).toList();
        var paired = results.stream().map(AdminTestInsights.Row::uplift).filter(Objects::nonNull).toList();
        var summary = new AdminTestInsights.Summary(
                (int) results.stream().filter(AdminTestInsights.Row::preSubmitted).count(),
                (int) results.stream().filter(AdminTestInsights.Row::postSubmitted).count(),
                average(results.stream().map(AdminTestInsights.Row::preScore)),
                average(results.stream().map(AdminTestInsights.Row::postScore)), average(paired),
                (int) paired.stream().filter(u -> u > 0).count(), paired.size());
        return new AdminTestInsights(summary, chapters, results);
    }

    private AdminQuizInsights buildQuizzes(AdminInsightsRequest r) {
        var submissions = repository.quizSubmissions(r.projectId(), r.groupId());
        var byQuiz = groupBy(submissions, AdminQuizInsights.Submission::quizId);
        var quizzes = repository.quizzes(r.projectId()).stream().map(q -> {
            var list = byQuiz.getOrDefault(q.id(), List.of());
            return new AdminQuizInsights.QuizRow(q.id(), q.name(), q.chapter(), q.questionCount(), list.size(),
                    (int) list.stream().map(s -> s.learner().accessId()).distinct().count(),
                    average(list.stream().map(AdminQuizInsights.Submission::score)),
                    average(bestScores(list)), list.stream().map(AdminQuizInsights.Submission::score)
                            .max(Integer::compare).orElse(null));
        }).toList();
        var best = bestScores(submissions);
        var distribution = BUCKETS.stream().map(b -> new AdminQuizInsights.Bucket(b.label(), b.min(), b.max(),
                (int) best.stream().filter(s -> s >= b.min() && s <= b.max()).count())).toList();
        var summary = new AdminQuizInsights.Summary(submissions.size(),
                (int) submissions.stream().map(s -> s.learner().accessId()).distinct().count(),
                average(submissions.stream().map(AdminQuizInsights.Submission::score)), average(best),
                (int) best.stream().filter(s -> s == 100).count());
        return new AdminQuizInsights(summary, distribution, quizzes, submissions);
    }

    // Each learner's best attempt per quiz, so retries don't drag an average down.
    private static List<Integer> bestScores(List<AdminQuizInsights.Submission> submissions) {
        return new ArrayList<>(submissions.stream().collect(Collectors.toMap(
                s -> s.learner().accessId() + "|" + s.quizId(), AdminQuizInsights.Submission::score, Math::max))
                .values());
    }

    private static AdminReviewInsights.Ratings averageRatings(List<AdminReviewInsights.Response> list) {
        return new AdminReviewInsights.Ratings(avg(list, AdminReviewInsights.Ratings::trainerClarity),
                avg(list, AdminReviewInsights.Ratings::trainerMastery),
                avg(list, AdminReviewInsights.Ratings::trainerResponsiveness),
                avg(list, AdminReviewInsights.Ratings::trainerEngagement),
                avg(list, AdminReviewInsights.Ratings::materialRelevance),
                avg(list, AdminReviewInsights.Ratings::materialFlow),
                avg(list, AdminReviewInsights.Ratings::materialDepth),
                avg(list, AdminReviewInsights.Ratings::learningValue));
    }

    private static Double avg(List<AdminReviewInsights.Response> list,
            Function<AdminReviewInsights.Ratings, Double> field) {
        return average(list.stream().map(response -> field.apply(response.ratings())));
    }

    private static int countReview(List<AdminGuidedInsights.Row> rows, AdminGuidedInsights.ReviewStatus status) {
        return (int) rows.stream().filter(row -> row.reviewStatus() == status).count();
    }

    private static int countFlags(Collection<AdminPreAssessmentInsights.Flags> flags,
            Predicate<AdminPreAssessmentInsights.Flags> test) {
        return (int) flags.stream().filter(test).count();
    }

    private static <T, K> Map<K, List<T>> groupBy(List<T> items, Function<T, K> key) {
        return items.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }
}
