package com.ailene.lms.sponsor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

// Shared maths and UTC calendar helpers for the sponsor dashboards.
final class SponsorMetrics {

    // Rupiah value of one hour saved, shared by every ROI figure.
    static final long ROI_VALUE_PER_HOUR = 250_000L;
    // Working hours per year used to express saved hours as full-time equivalents.
    static final long WORK_HOURS_PER_YEAR = 1_760L;

    static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private SponsorMetrics() {
    }

    static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    static int pct(long count, long total) {
        return total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
    }

    // Saved hours for one use case submission, ignoring incomplete or negative pairs.
    static double hoursSaved(Double hoursWithoutAi, Double hoursWithAi) {
        if (hoursWithoutAi == null || hoursWithAi == null) {
            return 0;
        }
        double saved = hoursWithoutAi - hoursWithAi;
        return saved > 0 ? saved : 0;
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // dayjs treats Sunday as the first day of the week; keep the same buckets.
    static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }
}
