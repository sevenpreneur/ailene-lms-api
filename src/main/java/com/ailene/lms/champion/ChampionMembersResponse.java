package com.ailene.lms.champion;

import java.util.List;

public record ChampionMembersResponse(TeamStats stats, List<TeamMemberItem> list) {
}
