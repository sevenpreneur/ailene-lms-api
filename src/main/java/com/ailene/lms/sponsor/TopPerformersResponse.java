package com.ailene.lms.sponsor;

import java.util.List;

public record TopPerformersResponse(int total, List<PerformerItem> list) {
}
