package com.ailene.lms.sponsor;

import java.util.UUID;

public record MemberUserRef(UUID id, String fullName, String email, String avatar) {
}
