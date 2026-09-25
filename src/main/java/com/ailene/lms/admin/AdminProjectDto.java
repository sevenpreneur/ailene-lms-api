package com.ailene.lms.admin;

public record AdminProjectDto(String id, String name, String companyName, long groupCount, long memberCount) {

    public static AdminProjectDto from(AdminProjectProjection p) {
        return new AdminProjectDto(p.getId(), p.getName(), p.getCompanyName(), p.getGroupCount(),
                p.getMemberCount());
    }
}
