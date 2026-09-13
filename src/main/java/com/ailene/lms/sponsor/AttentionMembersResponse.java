package com.ailene.lms.sponsor;

import java.util.List;

public record AttentionMembersResponse(int laggingCount, List<AttentionMember> members) {
}
