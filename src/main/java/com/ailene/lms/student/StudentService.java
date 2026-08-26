package com.ailene.lms.student;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.access.LeaderboardRowProjection;
import com.ailene.lms.access.LevelProgressProjection;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.prompt.PromptCompetencyProjection;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.usecase.UseCaseAchievementProjection;
import com.ailene.lms.usecase.UseCaseCompetencyProjection;
import com.ailene.lms.usecase.UseCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final List<String> TIER_NAMES = List.of("Beginner", "Explorer", "Operator", "Builder", "Master");
    private static final int COMPETENCY_WINDOW_DAYS = 90;

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final QuizRepository quizRepository;
    private final PromptRepository promptRepository;
    private final UseCaseRepository useCaseRepository;

    public StudentStatusResponse getStatus(String jwt, StudentStatusRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        return accessRepository.findStudentStatus(userId, request.projectId())
                .map(StudentStatusResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }

    public LevelProgressResponse getLevelProgress(String jwt, LevelProgressRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        LevelProgressProjection progress = accessRepository.findLevelProgress(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        long promptApproved = promptRepository.countApprovedSubmissions(request.projectId(), progress.getAccessId());
        List<UseCaseAchievementProjection> useCaseRows = useCaseRepository.findApprovedAchievements(
                request.projectId(), progress.getAccessId());

        double hoursSaved = 0;
        Map<String, String> toolsSeen = new LinkedHashMap<>();
        for (UseCaseAchievementProjection row : useCaseRows) {
            if (row.getHoursWithAi() != null && row.getHoursWithoutAi() != null) {
                double saved = row.getHoursWithoutAi() - row.getHoursWithAi();
                if (saved > 0) {
                    hoursSaved += saved;
                }
            }
            addTools(toolsSeen, row.getAiTool());
        }

        long tasksRequired = progress.getTasksRequired() == null ? 0 : progress.getTasksRequired();
        long tasksDone = progress.getTasksDone() == null ? 0 : progress.getTasksDone();

        return new LevelProgressResponse(progress.getXpCount() == null ? 0 : progress.getXpCount(),
                progress.getCurrentLevelNumber(), progress.getCurrentLevelName(), (int) tasksRequired,
                (int) tasksDone, tasksDone >= tasksRequired, useCaseRows.size(), (int) promptApproved,
                round1(hoursSaved), new ArrayList<>(toolsSeen.values()));
    }

    public CompetencyProfileResponse getCompetencyProfile(String jwt, CompetencyRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        String accessId = access.getId();
        String projectId = request.projectId();

        StudentStatusProjection status = accessRepository.findStudentStatus(userId, projectId).orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();

        Instant windowStart = Instant.now().minus(COMPETENCY_WINDOW_DAYS, ChronoUnit.DAYS);

        Double avgBestScore = quizRepository.findAverageBestScore(projectId, accessId);
        double aiFoundation = clamp((avgBestScore == null ? 0 : avgBestScore) / 20.0, 0, 5);

        double promptingQuality = computePromptingQuality(projectId, accessId, windowStart);

        List<UseCaseCompetencyProjection> useCaseRows = useCaseRepository.findCompetencySubmissions(projectId,
                accessId);
        double toolFluency = computeToolFluency(useCaseRows);
        double useCaseDiversity = computeUseCaseDiversity(useCaseRows);
        double aiHabit = computeAiHabit(useCaseRows, projectId, accessId, windowStart);

        double agenticCapabilities = 0;

        List<CompetencyDimension> dimensions = List.of(
                new CompetencyDimension("ai_foundation", "AI Foundation", round1(aiFoundation)),
                new CompetencyDimension("prompting_quality", "Prompting Quality", round1(promptingQuality)),
                new CompetencyDimension("tool_fluency", "Tool Fluency", round1(toolFluency)),
                new CompetencyDimension("use_case_diversity", "Use Case Diversity", round1(useCaseDiversity)),
                new CompetencyDimension("ai_habit", "AI Habit", round1(aiHabit)),
                new CompetencyDimension("agentic_capabilities", "Agentic Capabilities",
                        round1(agenticCapabilities)));

        double avg = round1(dimensions.stream().mapToDouble(CompetencyDimension::score).average().orElse(0));

        int tierNumber = avg < 1 ? 0 : avg < 2 ? 1 : avg < 3 ? 2 : avg < 4 ? 3 : 4;
        String tierName = TIER_NAMES.get(tierNumber);
        NextTier nextTier = tierNumber >= 4 ? null
                : new NextTier(tierNumber + 1, TIER_NAMES.get(tierNumber + 1), round1(tierNumber + 1 - avg));

        return new CompetencyProfileResponse(dimensions, avg, currentLevelNumber, tierNumber, tierName, nextTier);
    }

    public LeaderboardResponse getLeaderboard(String jwt, LeaderboardRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        GroupSummaryProjection summary = accessRepository.findGroupSummary(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        if (summary.getGroupId() == null) {
            return new LeaderboardResponse(null, 0, 0, List.of());
        }

        List<LeaderboardRowProjection> rows = accessRepository.findGroupLeaderboard(request.projectId(),
                summary.getGroupId());

        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        int myRank = 0;
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRowProjection row = rows.get(i);
            int rank = i + 1;
            boolean isMe = row.getAccessId().equals(summary.getAccessId());
            if (isMe) {
                myRank = rank;
            }
            leaderboard.add(new LeaderboardEntry(rank, row.getAccessId(), row.getFullName(), row.getAvatar(),
                    row.getTotalXp() == null ? 0 : row.getTotalXp(), isMe));
        }
        if (myRank == 0) {
            myRank = leaderboard.size();
        }

        return new LeaderboardResponse(new GroupSummary(summary.getGroupId(), summary.getGroupName()), myRank,
                leaderboard.size(), leaderboard);
    }

    private double computePromptingQuality(String projectId, String accessId, Instant windowStart) {
        List<PromptCompetencyProjection> rows = promptRepository.findCompetencySubmissions(projectId, accessId);

        List<RubricEntry> scored = new ArrayList<>();
        for (PromptCompetencyProjection row : rows) {
            if (row.getRubricSpecificity() == null || row.getRubricContext() == null
                    || row.getRubricConstraints() == null || row.getRubricExamples() == null
                    || row.getRubricIteration() == null) {
                continue;
            }
            double mean = (row.getRubricSpecificity() + row.getRubricContext() + row.getRubricConstraints()
                    + row.getRubricExamples() + row.getRubricIteration()) / 5.0;
            scored.add(new RubricEntry(row.getReviewedAt(), mean));
        }
        scored.sort((a, b) -> {
            long ta = a.reviewedAt() == null ? 0 : a.reviewedAt().toEpochMilli();
            long tb = b.reviewedAt() == null ? 0 : b.reviewedAt().toEpochMilli();
            return Long.compare(tb, ta);
        });

        List<RubricEntry> inWindow = scored.stream()
                .filter(e -> e.reviewedAt() != null && !e.reviewedAt().isBefore(windowStart))
                .toList();
        List<RubricEntry> relevant = inWindow.size() >= 3 ? inWindow : scored.stream().limit(3).toList();

        return relevant.isEmpty() ? 0
                : clamp(relevant.stream().mapToDouble(RubricEntry::mean).average().orElse(0), 0, 5);
    }

    private double computeToolFluency(List<UseCaseCompetencyProjection> rows) {
        Set<String> tools = new HashSet<>();
        for (UseCaseCompetencyProjection row : rows) {
            addToolKeys(tools, row.getAiTool());
        }
        int distinctTools = tools.size();
        double toolBreadth = clamp(distinctTools, 0, 5);

        long withProof = rows.stream()
                .filter(r -> r.getOutcomeProof() != null && !r.getOutcomeProof().isBlank())
                .count();
        double artifact = rows.isEmpty() ? 0 : clamp((withProof * 1.0 / rows.size()) * 5, 0, 5);

        double multiModel = distinctTools >= 3 ? 5 : distinctTools == 2 ? 3 : 0;

        return clamp(0.4 * toolBreadth + 0.4 * artifact + 0.2 * multiModel, 0, 5);
    }

    private double computeUseCaseDiversity(List<UseCaseCompetencyProjection> rows) {
        Set<String> distinctTypes = new HashSet<>();
        for (UseCaseCompetencyProjection row : rows) {
            if (row.getType() != null) {
                distinctTypes.add(row.getType());
            }
        }
        double diversityBase = Math.min(5, distinctTypes.size() * 0.7);

        long accepted = rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsAccepted())).count();
        double outcomeBonus = Math.min(1, accepted * 0.25);

        return clamp(diversityBase + outcomeBonus, 0, 5);
    }

    private double computeAiHabit(List<UseCaseCompetencyProjection> rows, String projectId, String accessId,
            Instant windowStart) {
        double totalHoursSaved = 0;
        for (UseCaseCompetencyProjection row : rows) {
            if (row.getHoursWithAi() == null || row.getHoursWithoutAi() == null) {
                continue;
            }
            double saved = row.getHoursWithoutAi() - row.getHoursWithAi();
            if (saved > 0) {
                totalHoursSaved += saved;
            }
        }
        double hoursScore = clamp(totalHoursSaved / 8.0, 0, 5);

        long activeDays = accessRepository.countActiveDays(projectId, accessId, windowStart);
        double consistency = clamp((activeDays / (double) COMPETENCY_WINDOW_DAYS) * 5, 0, 5);

        return clamp(0.6 * hoursScore + 0.4 * consistency, 0, 5);
    }

    private void addTools(Map<String, String> toolsSeen, String aiTool) {
        if (aiTool == null) {
            return;
        }
        for (String raw : aiTool.split(",")) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            toolsSeen.putIfAbsent(trimmed.toLowerCase(), trimmed);
        }
    }

    private void addToolKeys(Set<String> tools, String aiTool) {
        if (aiTool == null) {
            return;
        }
        for (String raw : aiTool.split(",")) {
            String trimmed = raw.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                tools.add(trimmed);
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private record RubricEntry(Instant reviewedAt, double mean) {
    }
}
