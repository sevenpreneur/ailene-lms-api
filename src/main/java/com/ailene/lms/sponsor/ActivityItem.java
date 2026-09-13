package com.ailene.lms.sponsor;

import java.time.Instant;

public record ActivityItem(String type, String actor, String action, String meta, String time, Instant at) {
}
