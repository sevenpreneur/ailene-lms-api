package com.ailene.lms.access;

public record ProjectAccessDto(String id, String name, String avatar, Integer groupId,
                               String groupName, String role, Boolean hasPreAssessment) {

    public static ProjectAccessDto from(ProjectAccessProjection projection) {
        return new ProjectAccessDto(projection.getId(), projection.getName(),
                projection.getAvatar(), projection.getGroupId(), projection.getGroupName(),
                projection.getRole(), projection.getHasPreAssessment());
    }
}
