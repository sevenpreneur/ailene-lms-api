package com.ailene.lms.champion;

import java.util.UUID;

public record TeamMemberUser(UUID id, String fullName, String email, String avatar) {
}
