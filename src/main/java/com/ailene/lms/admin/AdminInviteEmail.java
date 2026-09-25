package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;

public record AdminInviteEmail(String subject, String text, String html) {

    public static AdminInviteEmail build(String fullName, String email, String password, String projectName,
            AccessRole role, String groupName, String accessUrl) {
        String roleLabel = switch (role) {
            case champion -> "Champion";
            case student -> "Peserta";
            case sponsor -> "Sponsor";
        };
        String greeting = fullName == null || fullName.isBlank() ? "Halo," : "Halo " + fullName + ",";
        String subject = "Undangan ke " + projectName + " di Ailene LMS";
        boolean hasPassword = password != null && !password.isEmpty();

        String loginText = hasPassword
                ? String.join("\n", "Masuk dengan data berikut, atau dengan akun Google yang memakai alamat email ini:",
                        "Email: " + email, "Password: " + password, "", "Buka Ailene LMS:", accessUrl)
                : String.join("\n", "Masuk dengan akun Google yang memakai alamat email ini:", accessUrl);
        String text = String.join("\n", greeting, "",
                "Anda diundang ke program " + projectName + " di Ailene LMS sebagai " + roleLabel
                        + " di grup " + groupName + ".",
                "", loginText, "", "Salam,", "Tim Ailene");

        String credentialsHtml = !hasPassword ? "" : """
                  <p>Masuk dengan data berikut, atau dengan akun Google yang memakai alamat email ini:</p>
                  <table style="border-collapse:collapse;margin:0 0 8px">
                    <tr><td style="padding:4px 16px 4px 0;color:#6b7280">Email</td><td style="padding:4px 0"><strong>%s</strong></td></tr>
                    <tr><td style="padding:4px 16px 4px 0;color:#6b7280">Password</td><td style="padding:4px 0;font-family:monospace;font-size:15px"><strong>%s</strong></td></tr>
                  </table>
                """.formatted(escape(email), escape(password));
        String hintHtml = hasPassword ? "Jika tombol tidak berfungsi, buka tautan berikut:"
                : "Masuk dengan akun Google yang memakai alamat email ini. Jika tombol tidak berfungsi, buka tautan berikut:";

        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937;line-height:1.6">
                  <p>%s</p>
                  <p>Anda diundang ke program <strong>%s</strong> di Ailene LMS sebagai <strong>%s</strong> di grup <strong>%s</strong>.</p>
                %s  <p style="margin:28px 0">
                    <a href="%s" style="background:#111827;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:8px;display:inline-block">Buka Ailene LMS</a>
                  </p>
                  <p style="font-size:14px;color:#6b7280">%s<br><a href="%s" style="color:#6b7280">%s</a></p>
                  <p>Salam,<br>Tim Ailene</p>
                </div>
                """.formatted(escape(greeting), escape(projectName), roleLabel, escape(groupName), credentialsHtml,
                escape(accessUrl), hintHtml, escape(accessUrl), escape(accessUrl));

        return new AdminInviteEmail(subject, text, html);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
