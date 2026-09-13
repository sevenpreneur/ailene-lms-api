package com.ailene.lms.sponsor;

import java.util.List;

public record OrganizationLeaderboardResponse(short maxScore, List<DepartmentPerformance> list) {
}
