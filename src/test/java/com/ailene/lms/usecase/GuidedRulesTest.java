package com.ailene.lms.usecase;

import com.ailene.lms.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class GuidedRulesTest {
    @Test
    void rejectsNikAccountsAndBulkContactsButAcceptsAnonymizedExamples() {
        for (String text : List.of("NIK 1234567890123456", "Rekening: 1234567890",
                "a@example.test b@example.test c@example.test",
                "081234567890 082234567890 083234567890")) {
            assertThatThrownBy(() -> GuidedRules.requireSafe(text)).isInstanceOf(BadRequestException.class);
        }
        assertThatCode(() -> GuidedRules.requireSafe("Pelanggan A, target 50 juta dan realisasi 45 juta."))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsScriptUrlsAndUserInfoButDoesNotFetchFileUrls() {
        for (String url : List.of("javascript:alert(1)", "https://user:pass@example.com/proof", "file:///tmp/proof")) {
            assertThatThrownBy(() -> GuidedRules.requireLink(url, false)).isInstanceOf(BadRequestException.class);
        }
        assertThatCode(() -> GuidedRules.requireLink("https://storage.example.test/example.xlsx", true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> GuidedRules.requireLink("https://storage.example.test/example.exe", true))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void calculatesSavingsFromExactMinutesAndNeverNegative() {
        GuidedSession session = new GuidedSession();
        session.setMinutesWithoutAi(new BigDecimal("90"));
        assertThat(GuidedRules.hoursSaved(session, new BigDecimal("0.5"))).isEqualByComparingTo("1.00");
        assertThat(GuidedRules.hoursSaved(session, new BigDecimal("2.5"))).isEqualByComparingTo("0.00");
        session.setMinutesWithoutAi(new BigDecimal("1"));
        assertThat(GuidedRules.hoursSaved(session, new BigDecimal("0.01"))).isEqualByComparingTo("0.01");
    }

    @Test
    void skippedStepsNeedAnActualReason() {
        GuidedStep empty = new GuidedStep(1, 1, "Cek", "Cek", "Manual", "", 1, "Cek", false, " ", null, null);
        GuidedStep skipped = new GuidedStep(1, 1, "Cek", "Cek", "Manual", "", 1, "Cek", false, "Tidak diperlukan", null, null);
        assertThat(GuidedRules.completed(List.of(empty))).isFalse();
        assertThat(GuidedRules.completed(List.of(skipped))).isTrue();
        assertThat(GuidedRules.completed(List.of())).isFalse();
    }
}
