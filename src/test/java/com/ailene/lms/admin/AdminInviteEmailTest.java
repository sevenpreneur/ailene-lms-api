package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInviteEmailTest {

    @Test
    void build_carriesCtaUrlAndRole() {
        String url = "https://lms.ailene.id/V7rdgcYkq9PHQZkwvoA-F/student";

        AdminInviteEmail email = AdminInviteEmail.build("Rani", "Hutama Karya AI Training", AccessRole.student,
                "Finance", url);

        assertThat(email.subject()).isEqualTo("Undangan ke Hutama Karya AI Training di Ailene LMS");
        assertThat(email.text()).contains("Halo Rani,", "sebagai Peserta di grup Finance", url);
        assertThat(email.html()).contains("href=\"" + url + "\"", "<strong>Peserta</strong>");
    }

    @Test
    void build_escapesUserSuppliedText() {
        AdminInviteEmail email = AdminInviteEmail.build("<b>Rani</b>", "R&D", AccessRole.champion, "A \"B\"",
                "https://lms.ailene.id/p/champion");

        assertThat(email.html()).contains("&lt;b&gt;Rani&lt;/b&gt;", "R&amp;D", "A &quot;B&quot;")
                .doesNotContain("<b>Rani</b>");
    }
}
