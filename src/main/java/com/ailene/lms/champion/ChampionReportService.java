package com.ailene.lms.champion;

import com.ailene.lms.common.Status;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChampionReportService {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd MMM yyyy",
            Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy",
            Locale.ENGLISH);
    private static final int SENT_REPORT_COUNT = 3;
    private static final int NOTE_NAME_LIMIT = 3;

    private final ChampionAccessGuard championAccessGuard;
    private final ChampionRepository championRepository;
    private final LevelRepository levelRepository;

    public ChampionReportResponse getReport(String jwt, ChampionReportRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        ReportPeriod period = request.periodOrDefault();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStartDate = period == ReportPeriod.monthly ? today.withDayOfMonth(1)
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate previousStartDate = period == ReportPeriod.monthly ? periodStartDate.minusMonths(1)
                : periodStartDate.minusWeeks(1);
        Instant periodStart = periodStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TeamMemberProjection> members = championRepository.findTeamMembers(request.projectId(),
                champion.groupId(), champion.accessId());
        List<String> accessIds = members.stream().map(TeamMemberProjection::getAccessId).toList();

        int activeMembers = (int) members.stream()
                .filter(member -> member.getLastActiveAt() != null
                        && member.getLastActiveAt().isAfter(periodStart))
                .count();

        int acceptedUseCases = 0;
        double hoursSaved = 0;
        if (!accessIds.isEmpty()) {
            for (TeamUseCaseProjection row : championRepository.findTeamUseCaseSubmissions(accessIds)) {
                if (row.getSubmittedAt() == null || !row.getSubmittedAt().isAfter(periodStart)
                        || !Boolean.TRUE.equals(row.getAccepted())) {
                    continue;
                }
                acceptedUseCases++;
                if (row.getHoursWithAi() != null && row.getHoursWithoutAi() != null
                        && row.getHoursWithoutAi() > row.getHoursWithAi()) {
                    hoursSaved += row.getHoursWithoutAi() - row.getHoursWithAi();
                }
            }
        }

        int acceptedPrompts = (int) championRepository.findPromptReviewQueue(champion.accessId()).stream()
                .filter(row -> row.getSubmittedAt() != null && row.getSubmittedAt().isAfter(periodStart)
                        && Boolean.TRUE.equals(row.getAccepted()))
                .count();

        List<Level> levels = levelRepository.findByStatusOrderByLevelNumberAsc(Status.active);
        List<TeamLevelHistoryProjection> history = accessIds.isEmpty() ? List.of()
                : championRepository.findTeamLevelHistory(accessIds);

        List<LevelMovement> movements = buildMovements(levels, members, history, periodStart);
        int totalLevelUps = movements.stream().mapToInt(LevelMovement::count).sum();

        SponsorRecipientProjection sponsor = championRepository.findProjectSponsor(request.projectId()).stream()
                .findFirst().orElse(null);
        String recipientName = sponsor == null ? "Sponsor" : sponsor.getFullName();

        String teamName = champion.groupName() == null ? "Tim Champion" : champion.groupName();
        String title = period == ReportPeriod.monthly
                ? "Laporan Bulanan - " + today.format(MONTH_YEAR)
                : "Laporan Mingguan - " + periodStartDate.format(DAY_MONTH) + " - "
                        + today.format(DAY_MONTH_YEAR);

        int acceptedSubmissions = acceptedUseCases + acceptedPrompts;
        int lowActivity = members.size() - activeMembers;

        return new ChampionReportResponse(period, Instant.now(),
                new ReportInfo(title, teamName, "Champion", "draft_auto_generated"),
                sponsor == null ? null
                        : new ReportRecipient(sponsor.getAccessId(), sponsor.getFullName(), sponsor.getAvatar(),
                                sponsor.getJobTitle()),
                new ReportMetrics(activeMembers, members.size(), pct(activeMembers, members.size()),
                        acceptedSubmissions, round1(hoursSaved), totalLevelUps),
                movements,
                buildNarrative(teamName, period, acceptedSubmissions, hoursSaved, totalLevelUps, lowActivity),
                buildSentReports(period, today, recipientName),
                previousStartDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private List<LevelMovement> buildMovements(List<Level> levels, List<TeamMemberProjection> members,
            List<TeamLevelHistoryProjection> history, Instant periodStart) {
        List<LevelMovement> movements = new ArrayList<>();
        for (int i = 0; i < levels.size() - 1; i++) {
            short fromLevel = levels.get(i).getLevelNumber();
            short toLevel = levels.get(i + 1).getLevelNumber();

            List<String> names = new ArrayList<>();
            for (TeamMemberProjection member : members) {
                boolean reached = history.stream()
                        .anyMatch(row -> row.getAccessId().equals(member.getAccessId())
                                && row.getLevelNumber() != null && row.getLevelNumber() == toLevel
                                && row.getReachedAt() != null && row.getReachedAt().isAfter(periodStart));
                if (reached) {
                    names.add(member.getFullName());
                }
            }

            String note = names.isEmpty() ? "Belum ada perpindahan"
                    : String.join(", ", names.stream().limit(NOTE_NAME_LIMIT).toList())
                            + (names.size() > NOTE_NAME_LIMIT ? ", +" + (names.size() - NOTE_NAME_LIMIT) : "");

            movements.add(new LevelMovement("L" + fromLevel, "L" + toLevel, names.size(), note));
        }
        return movements;
    }

    private String buildNarrative(String teamName, ReportPeriod period, int acceptedSubmissions, double hoursSaved,
            int totalLevelUps, int lowActivity) {
        String unit = period == ReportPeriod.monthly ? "bulan" : "minggu";
        List<String> parts = new ArrayList<>();
        parts.add("Momentum tim " + teamName + " " + unit + " ini berjalan "
                + (acceptedSubmissions > 0 ? "positif" : "stabil") + ".");
        parts.add(acceptedSubmissions + " submission diterima dan estimasi " + round1(hoursSaved)
                + " jam kerja berhasil dihemat.");
        parts.add(totalLevelUps > 0
                ? totalLevelUps + " anggota naik level; fokus berikutnya menjaga konsistensi praktik."
                : "Belum ada kenaikan level; fokus berikutnya mempercepat penyelesaian gate level.");
        parts.add(lowActivity > 0 ? lowActivity + " anggota perlu coaching 1-on-1 karena aktivitas rendah."
                : "Seluruh anggota aktif dalam periode ini.");

        return String.join(" ", parts);
    }

    private List<SentReport> buildSentReports(ReportPeriod period, LocalDate today, String recipient) {
        List<SentReport> reports = new ArrayList<>();
        for (int offset = 1; offset <= SENT_REPORT_COUNT; offset++) {
            LocalDate date = period == ReportPeriod.monthly
                    ? today.minusMonths(offset).with(TemporalAdjusters.lastDayOfMonth())
                    : today.minusWeeks(offset).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            String title = period == ReportPeriod.monthly
                    ? "Laporan Bulanan - " + date.format(SHORT_MONTH_YEAR)
                    : "Laporan Mingguan - " + date.format(DAY_MONTH_YEAR);

            reports.add(new SentReport(period.name() + "-" + offset, title,
                    date.atStartOfDay(ZoneOffset.UTC).toInstant(), recipient));
        }
        return reports;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static int pct(int count, int total) {
        return total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
    }
}
