package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInviteEmailTest {

    private static final String URL = "https://lms.ailene.id/V7rdgcYkq9PHQZkwvoA-F/student";

    @Test
    void build_withoutPassword_pointsToGoogleSignIn() {
        AdminInviteEmail email = AdminInviteEmail.build("Rani", "rani@example.com", null,
                "Hutama Karya AI Training", AccessRole.student, "Finance", URL);

        assertThat(email.subject()).isEqualTo("Undangan ke Hutama Karya AI Training di Ailene LMS");
        assertThat(email.text()).contains("Halo Rani,", "sebagai Peserta di grup Finance",
                "Masuk dengan akun Google", URL).doesNotContain("Password");
        assertThat(email.html()).contains("href=\"" + URL + "\"", "<strong>Peserta</strong>")
                .doesNotContain("Password");
    }

    @Test
    void build_withPassword_includesTheCredentials() {
        AdminInviteEmail email = AdminInviteEmail.build("Rani", "rani@example.com", "Rahasia#2026",
                "Hutama Karya AI Training", AccessRole.student, "Finance", URL);

        assertThat(email.text()).contains("Email: rani@example.com", "Password: Rahasia#2026", URL);
        assertThat(email.html()).contains("<strong>rani@example.com</strong>", "<strong>Rahasia#2026</strong>");
    }

    @Test
    void build_escapesUserSuppliedText() {
        AdminInviteEmail email = AdminInviteEmail.build("<b>Rani</b>", "r@x.id", "a<b>&\"c\"d",
                "R&D", AccessRole.champion, "A \"B\"", "https://lms.ailene.id/p/champion");

        assertThat(email.html()).contains("&lt;b&gt;Rani&lt;/b&gt;", "R&amp;D", "A &quot;B&quot;",
                "a&lt;b&gt;&amp;&quot;c&quot;d").doesNotContain("<b>Rani</b>");
    }
}
