package com.ailene.lms.admin;

import com.ailene.lms.access.AccessRole;

public record AdminInviteEmail(String subject, String text, String html) {

    public static AdminInviteEmail build(String fullName, String projectName, AccessRole role, String groupName,
            String accessUrl) {
        String roleLabel = switch (role) {
            case champion -> "Champion";
            case student -> "Peserta";
            case sponsor -> "Sponsor";
        };
        String greeting = fullName == null || fullName.isBlank() ? "Halo," : "Halo " + fullName + ",";
        String subject = "Undangan ke " + projectName + " di Ailene LMS";

        String text = String.join("\n", greeting, "",
                "Anda diundang ke program " + projectName + " di Ailene LMS sebagai " + roleLabel
                        + " di grup " + groupName + ".",
                "", "Masuk dengan akun Google yang memakai alamat email ini:", accessUrl, "", "Salam,", "Tim Ailene");

        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937;line-height:1.6">
                  <p>%s</p>
                  <p>Anda diundang ke program <strong>%s</strong> di Ailene LMS sebagai <strong>%s</strong> di grup <strong>%s</strong>.</p>
                  <p style="margin:28px 0">
                    <a href="%s" style="background:#111827;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:8px;display:inline-block">Buka Ailene LMS</a>
                  </p>
                  <p style="font-size:14px;color:#6b7280">Masuk dengan akun Google yang memakai alamat email ini. Jika tombol tidak berfungsi, buka tautan berikut:<br><a href="%s" style="color:#6b7280">%s</a></p>
                  <p>Salam,<br>Tim Ailene</p>
                </div>
                """.formatted(escape(greeting), escape(projectName), roleLabel, escape(groupName), escape(accessUrl),
                escape(accessUrl), escape(accessUrl));

        return new AdminInviteEmail(subject, text, html);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
