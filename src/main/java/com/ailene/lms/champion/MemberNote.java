package com.ailene.lms.champion;

import java.time.Instant;

public record MemberNote(Integer id, String text, Instant createdAt, String championName) {
}
