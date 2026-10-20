package com.ailene.lms.usecase;

import com.ailene.lms.common.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

final class GuidedRules {
    private static final Pattern NIK = Pattern.compile("(?<!\\d)(?:\\d[ -]?){15}\\d(?!\\d)");
    private static final Pattern ACCOUNT = Pattern.compile("(?i)(?:rekening|account|gaji|salary|npwp)[^\\n]{0,25}\\d[\\d .,-]{5,}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+62|62|0)8[0-9 -]{8,13}(?!\\d)");
    private static final Pattern FILE = Pattern.compile("(?i).*\\.(pdf|docx|xlsx|csv|png|jpe?g)$");

    private GuidedRules() {
    }

    static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static void requireSafe(String... texts) {
        String text = String.join("\n", java.util.Arrays.stream(texts).map(v -> v == null ? "" : v).toList());
        if (NIK.matcher(text).find() || ACCOUNT.matcher(text).find()
                || EMAIL.matcher(text).results().limit(3).count() >= 3
                || PHONE.matcher(text).results().limit(3).count() >= 3) {
            throw new BadRequestException("Data sensitif terdeteksi. Samarkan NIK, rekening, gaji, email, dan nomor telepon terlebih dahulu.");
        }
    }

    static void requireLink(String value, boolean example) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !(uri.getScheme().equalsIgnoreCase("https")
                        || (!example && uri.getScheme().equalsIgnoreCase("http")))
                    || (example && !FILE.matcher(uri.getPath()).matches())) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new BadRequestException(example
                    ? "Tautan contoh harus HTTPS dengan format PDF, DOCX, XLSX, CSV, PNG, atau JPG."
                    : "Bukti hasil harus berupa tautan HTTP atau HTTPS yang valid.");
        }
    }

    static boolean completed(List<GuidedStep> steps) {
        return !steps.isEmpty() && steps.stream().allMatch(s -> s.isDone() || clean(s.skippedReason()) != null);
    }

    static boolean generating(GuidedSession session) {
        return session.getGenerationToken() != null && session.getGenerationStartedAt() != null
                && session.getGenerationStartedAt().isAfter(OffsetDateTime.now().minusMinutes(5));
    }

    static void requireThrough(GuidedSession s, List<GuidedStep> steps, int target) {
        if (target >= 2 && (s.getDailyTask() == null || s.getDailyTask().strip().length() < 30 || s.getFrequency() == null)) {
            throw new BadRequestException("Isi pekerjaan minimal 30 karakter dan pilih frekuensi.");
        }
        if (target >= 3 && (!s.isSensitiveDataConfirmed()
                || (clean(s.getExampleText()) == null && clean(s.getExampleFileUrl()) == null))) {
            throw new BadRequestException("Tambahkan contoh dan konfirmasi penyamaran data sensitif.");
        }
        if (target >= 4 && (s.getMinutesWithoutAi() == null || s.getMinutesWithoutAi().signum() <= 0
                || clean(s.getCurrentProcess()) == null)) {
            throw new BadRequestException("Isi durasi lebih dari nol dan proses kerja sekarang.");
        }
        if (target >= 5 && steps.isEmpty()) {
            throw new BadRequestException("Buat workflow terlebih dahulu.");
        }
        if (target >= 6 && !completed(steps)) {
            throw new BadRequestException("Selesaikan setiap langkah atau isi alasan langkah yang dilewati.");
        }
    }

    static void requireOutcome(GuidedSubmitRequest r) {
        if (clean(r.outcomeProof()) == null || clean(r.description()) == null || clean(r.aiTool()) == null
                || r.type() == null || r.hoursWithAi() == null || r.hoursWithAi().signum() <= 0
                || r.hoursWithAi().compareTo(new BigDecimal("9999.99")) > 0
                || r.hoursWithAi().stripTrailingZeros().scale() > 2
                || clean(r.testimonial()) == null || r.testimonial().strip().length() < 50
                || r.helpfulnessRating() == null || r.helpfulnessRating() < 1 || r.helpfulnessRating() > 5
                || r.shareConsent() == null) {
            throw new BadRequestException("Lengkapi bukti, durasi, tool, tipe, hasil, testimoni minimal 50 karakter, rating, dan persetujuan publikasi.");
        }
        requireLink(r.outcomeProof(), false);
        requireSafe(r.description(), r.testimonial());
    }

    static BigDecimal hoursWithoutAi(GuidedSession session) {
        return session.getMinutesWithoutAi().divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal hoursSaved(GuidedSession session, BigDecimal hoursWithAi) {
        return session.getMinutesWithoutAi().divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP)
                .subtract(hoursWithAi).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
