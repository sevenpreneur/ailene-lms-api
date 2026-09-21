package com.ailene.lms.champion;

import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.sponsor.DepartmentRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChampionTeamService {

    private static final int AT_RISK_DAYS = 7;
    private static final int BEHIND_DAYS = 14;
    private static final int ACTIVITY_LIMIT = 8;
    private static final int QUIZ_ACTIVITY_LIMIT = 8;

    private final ChampionAccessGuard championAccessGuard;
    private final ChampionRepository championRepository;

    public ChampionMembersResponse getMembers(String jwt, ChampionMembersRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        Integer groupId = champion.groupId();

        if (request.groupId() != null && !request.groupId().equals(groupId)) {
            throw new ForbiddenException("You can only view the group you lead");
        }

        List<TeamMemberProjection> members = championRepository.findTeamMembers(request.projectId(), groupId,
                champion.accessId());
        if (members.isEmpty()) {
            return new ChampionMembersResponse(new TeamStats(0, 0, 0, 0, 0, 0, 0, 0), List.of());
        }

        List<String> accessIds = members.stream().map(TeamMemberProjection::getAccessId).toList();
        long totalTasks = championRepository.countLearningTasks(request.projectId(), groupId);

        Map<String, Long> tasksDone = new HashMap<>();
        for (AccessCountProjection row : championRepository.findTasksDone(accessIds)) {
            tasksDone.put(row.getAccessId(), row.getDoneCount() == null ? 0 : row.getDoneCount());
        }
        Map<String, Long> xpTotals = new HashMap<>();
        for (AccessTotalProjection row : championRepository.findXpTotals(accessIds)) {
            xpTotals.put(row.getAccessId(), row.getTotal() == null ? 0 : row.getTotal());
        }

        Map<String, Integer> acceptedUseCases = new HashMap<>();
        int submissionsSent = 0;
        double hoursSavedTotal = 0;
        Set<String> submitters = new HashSet<>();
        for (TeamUseCaseProjection row : championRepository.findTeamUseCaseSubmissions(accessIds)) {
            if (Boolean.TRUE.equals(row.getAccepted())) {
                acceptedUseCases.merge(row.getAccessId(), 1, Integer::sum);
            }
            if (row.getSubmittedAt() == null) {
                continue;
            }
            submissionsSent++;
            submitters.add(row.getAccessId());
            if (row.getHoursWithAi() != null && row.getHoursWithoutAi() != null
                    && row.getHoursWithoutAi() > row.getHoursWithAi()) {
                hoursSavedTotal += row.getHoursWithoutAi() - row.getHoursWithAi();
            }
        }

        Instant now = Instant.now();
        Instant weekStart = LocalDate.now(ZoneOffset.UTC)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TeamMemberItem> list = new ArrayList<>();
        for (TeamMemberProjection member : members) {
            long done = tasksDone.getOrDefault(member.getAccessId(), 0L);
            int progressPercent = totalTasks == 0 ? 0
                    : (int) Math.max(0, Math.min(100, Math.round((done * 100.0) / totalTasks)));

            list.add(new TeamMemberItem(member.getAccessId(),
                    new TeamMemberUser(member.getUserId(), member.getFullName(), member.getEmail(),
                            member.getAvatar()),
                    new TeamLevelRef(member.getLevelId(), member.getLevelNumber(), member.getLevelName()),
                    xpTotals.getOrDefault(member.getAccessId(), 0L), progressPercent,
                    acceptedUseCases.getOrDefault(member.getAccessId(), 0), member.getLastActiveAt(),
                    statusOf(member.getLastActiveAt(), now)));
        }

        list = list.stream()
                .sorted(Comparator.comparingInt((TeamMemberItem item) -> levelNumber(item)).reversed()
                        .thenComparing(Comparator.comparingLong(TeamMemberItem::totalXp).reversed())
                        .thenComparing((TeamMemberItem item) -> item.user().fullName()))
                .toList();

        TeamStats stats = new TeamStats(list.size(),
                (int) list.stream().filter(item -> "on_track".equals(item.status())).count(),
                (int) list.stream().filter(item -> "at_risk".equals(item.status())).count(),
                (int) list.stream().filter(item -> "behind".equals(item.status())).count(),
                (int) list.stream().filter(item -> item.lastActiveAt() != null
                        && !item.lastActiveAt().isBefore(weekStart)).count(),
                submissionsSent, submitters.size(), Math.round(hoursSavedTotal));

        return new ChampionMembersResponse(stats, list);
    }

    public MemberDetailResponse getMemberDetail(String jwt, MemberDetailRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        Integer groupId = champion.groupId();

        TeamMemberProjection member = championRepository
                .findTeamMembers(request.projectId(), groupId, champion.accessId()).stream()
                .filter(row -> row.getAccessId().equals(request.memberAccessId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You can only access members in the group you lead"));

        List<MemberQuizProjection> quizzes = championRepository
                .findMemberQuizSubmissions(request.memberAccessId());
        List<MemberSubmissionProjection> promptSubs = championRepository
                .findMemberPromptSubmissions(request.memberAccessId(), champion.accessId());
        List<MemberSubmissionProjection> useCaseSubs = championRepository
                .findMemberUseCaseSubmissions(request.memberAccessId(), champion.accessId());

        Map<String, Short> bestQuizScore = new HashMap<>();
        for (MemberQuizProjection quiz : quizzes) {
            short score = quiz.getScore() == null ? 0 : quiz.getScore();
            bestQuizScore.merge(quiz.getQuizId(), score, (a, b) -> a >= b ? a : b);
        }
        int avgQuiz = bestQuizScore.isEmpty() ? 0
                : (int) Math.round(bestQuizScore.values().stream().mapToInt(Short::intValue).average().orElse(0));

        GateProgressProjection gate = championRepository.findMemberGateProgress(request.memberAccessId()).stream()
                .findFirst().orElse(null);
        long gateTotal = gate == null || gate.getRequired() == null ? 0 : gate.getRequired();
        long gateDone = gate == null || gate.getDone() == null ? 0 : gate.getDone();
        int gatePercent = gateTotal == 0 ? 100 : (int) Math.round((gateDone * 100.0) / gateTotal);

        int streak = computeStreak(championRepository.findMemberActivityDays(request.memberAccessId()));

        List<MemberSubmissionProjection> submittedPrompts = promptSubs.stream()
                .filter(row -> row.getSubmittedAt() != null).toList();
        List<MemberSubmissionProjection> submittedUseCases = useCaseSubs.stream()
                .filter(row -> row.getSubmittedAt() != null).toList();
        int promptAccepted = (int) promptSubs.stream().filter(row -> Boolean.TRUE.equals(row.getAccepted()))
                .count();
        int useCaseAccepted = (int) useCaseSubs.stream().filter(row -> Boolean.TRUE.equals(row.getAccepted()))
                .count();

        Set<String> uniqueTools = new HashSet<>();
        for (MemberSubmissionProjection row : submittedUseCases) {
            if (row.getAiTool() == null) {
                continue;
            }
            for (String raw : row.getAiTool().split(",")) {
                String tool = raw.trim().toLowerCase();
                if (!tool.isEmpty()) {
                    uniqueTools.add(tool);
                }
            }
        }

        List<RadarDimension> dimensions = List.of(
                dimension("specificity", "Specificity", avgQuiz / 20.0),
                dimension("context", "Context", submittedPrompts.isEmpty() ? 0
                        : (promptAccepted / (double) submittedPrompts.size()) * 5),
                dimension("verification", "Verification", submittedUseCases.isEmpty() ? 0
                        : (useCaseAccepted / (double) submittedUseCases.size()) * 5),
                dimension("iteration", "Iteration", submittedPrompts.size() / 2.0),
                dimension("workflow", "Workflow", submittedUseCases.size()),
                dimension("tool", "Tool", uniqueTools.size()));

        List<MemberActivity> activities = buildActivities(quizzes, promptSubs, useCaseSubs);

        List<MemberNote> notes = championRepository.findMemberNotes(request.memberAccessId()).stream()
                .map(row -> new MemberNote(row.getId(), row.getText(), row.getCreatedAt(), row.getChampionName()))
                .toList();

        short currentLevelNumber = member.getLevelNumber() == null ? 0 : member.getLevelNumber();
        short nextLevelNumber = gate == null || gate.getNextLevelNumber() == null ? currentLevelNumber
                : gate.getNextLevelNumber();

        MemberGate memberGate = new MemberGate(currentLevelNumber, nextLevelNumber,
                gate == null ? null : gate.getNextLevelId(), (int) gateDone, (int) gateTotal, gatePercent,
                gatePercent >= 100,
                List.of(new GateRequirement(gateDone + " / " + gateTotal + " modul level " + currentLevelNumber
                        + " selesai", gateTotal > 0 && gateDone >= gateTotal),
                        new GateRequirement(avgQuiz > 0 ? "Avg quiz " + avgQuiz + "/100" : "Avg quiz belum ada",
                                avgQuiz >= 80),
                        new GateRequirement(useCaseAccepted + " use case diterima", useCaseAccepted > 0),
                        new GateRequirement(promptAccepted + " prompt diterima", promptAccepted > 0)));

        MemberDetailInfo info = new MemberDetailInfo(member.getAccessId(), member.getFullName(),
                member.getEmail(), member.getAvatar(), member.getJobTitle(),
                member.getGroupId() == null ? null : new DepartmentRef(member.getGroupId(), member.getGroupName()),
                new TeamLevelRef(member.getLevelId(), member.getLevelNumber(), member.getLevelName()),
                member.getJoinedAt(), member.getLastActiveAt());

        int submissionTotal = submittedPrompts.size() + submittedUseCases.size();

        return new MemberDetailResponse(info,
                new MemberDetailMetrics(gatePercent, streak, submissionTotal, avgQuiz),
                new MemberRadar(submissionTotal, dimensions), memberGate, activities, notes);
    }

    private List<MemberActivity> buildActivities(List<MemberQuizProjection> quizzes,
            List<MemberSubmissionProjection> promptSubs, List<MemberSubmissionProjection> useCaseSubs) {
        List<MemberActivity> activities = new ArrayList<>();
        for (MemberSubmissionProjection row : useCaseSubs) {
            activities.add(submissionActivity("use-case-", "use_case", "Use case: ", row));
        }
        for (MemberSubmissionProjection row : promptSubs) {
            activities.add(submissionActivity("prompt-", "prompt", "Prompt: ", row));
        }
        for (MemberQuizProjection quiz : quizzes.stream().limit(QUIZ_ACTIVITY_LIMIT).toList()) {
            activities.add(new MemberActivity("quiz-" + quiz.getId(), "quiz", "Quiz: " + quiz.getQuizName(),
                    "Lulus - skor " + (quiz.getScore() == null ? 0 : quiz.getScore()) + "/100", "accepted",
                    quiz.getSubmittedAt()));
        }

        return activities.stream()
                .sorted(Comparator.comparing(MemberActivity::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(ACTIVITY_LIMIT)
                .toList();
    }

    private MemberActivity submissionActivity(String idPrefix, String type, String titlePrefix,
            MemberSubmissionProjection row) {
        boolean accepted = Boolean.TRUE.equals(row.getAccepted());
        String status = accepted ? "accepted" : row.getSubmittedAt() != null ? "submitted" : "assigned";
        String subtitle = accepted ? "Diterima"
                : row.getSubmittedAt() != null ? "Submitted - menunggu review" : "Belum submit";
        Instant occurredAt = row.getReviewedAt() != null ? row.getReviewedAt()
                : row.getSubmittedAt() != null ? row.getSubmittedAt() : row.getCreatedAt();

        return new MemberActivity(idPrefix + row.getId(), type, titlePrefix + row.getItemName(), subtitle, status,
                occurredAt);
    }

    private int computeStreak(List<ActivityDayProjection> rows) {
        Set<LocalDate> activeDays = new HashSet<>();
        for (ActivityDayProjection row : rows) {
            if (row.getAt() != null) {
                activeDays.add(row.getAt().atZone(ZoneOffset.UTC).toLocalDate());
            }
        }

        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        if (!activeDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static RadarDimension dimension(String key, String label, double rawScore) {
        double clamped = Math.max(0, Math.min(5, rawScore));
        return new RadarDimension(key, label, Math.round(clamped * 10) / 10.0);
    }

    private static String statusOf(Instant lastActiveAt, Instant now) {
        if (lastActiveAt == null) {
            return "behind";
        }
        long days = ChronoUnit.DAYS.between(lastActiveAt, now);
        if (days > BEHIND_DAYS) {
            return "behind";
        }
        return days > AT_RISK_DAYS ? "at_risk" : "on_track";
    }

    private static int levelNumber(TeamMemberItem item) {
        return item.currentLevel().levelNumber() == null ? 0 : item.currentLevel().levelNumber();
    }

}
