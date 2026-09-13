package com.ailene.lms.sponsor;

public record WorkforceMember(String accessId, MemberUserRef user, DepartmentRef department, String jobTitle,
        MemberLevelRef currentLevel, double score, int progressPercent, String segment, double hoursSavedWeekly,
        MemberStatus status) {
}
