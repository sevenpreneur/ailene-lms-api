package com.ailene.lms.access;

public record ProjectAccessDto(String projectId, String projectName, String projectAvatar, Integer groupId,
                               String groupName, String role) {

    public static ProjectAccessDto from(ProjectAccessProjection projection) {
        return new ProjectAccessDto(projection.getProjectId(), projection.getProjectName(),
                projection.getProjectAvatar(), projection.getGroupId(), projection.getGroupName(),
                projection.getRole());
    }
}
