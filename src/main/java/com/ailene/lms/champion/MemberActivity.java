package com.ailene.lms.champion;

import java.time.Instant;

public record MemberActivity(String id, String type, String title, String subtitle, String status,
        Instant occurredAt) {
}
