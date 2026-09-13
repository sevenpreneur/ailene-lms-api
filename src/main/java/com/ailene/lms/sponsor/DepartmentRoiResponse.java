package com.ailene.lms.sponsor;

import java.util.List;

public record DepartmentRoiResponse(long totalRoiAnnualized, List<DepartmentRoiItem> departments) {
}
