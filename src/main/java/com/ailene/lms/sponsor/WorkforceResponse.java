package com.ailene.lms.sponsor;

import java.util.List;

public record WorkforceResponse(int total, List<DepartmentRef> departments, List<WorkforceMember> list) {
}
