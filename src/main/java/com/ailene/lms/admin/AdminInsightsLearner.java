package com.ailene.lms.admin;

public record AdminInsightsLearner(String accessId, String role, String fullName, String email, String avatar,
        Integer groupId, String groupName) {
}
