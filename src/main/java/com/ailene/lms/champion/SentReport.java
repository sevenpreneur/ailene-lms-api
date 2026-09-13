package com.ailene.lms.champion;

import java.time.Instant;

public record SentReport(String id, String title, Instant sentAt, String recipient) {
}
