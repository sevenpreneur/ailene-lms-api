package com.ailene.lms.sponsor;

import java.util.List;

public record GroupLevelDistributionResponse(int totalMembers, List<GroupLevelItem> levels) {
}
