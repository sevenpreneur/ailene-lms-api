package com.ailene.lms.admin;

public record InviteMemberResponse(AdminMemberDto member, boolean emailSent, String accessUrl) {
}
