package com.ailene.lms.champion;

import java.util.List;

public record TeamMemberBaseline(String accessId, String name, String avatar, double avg,
        List<TeamPillarScore> pillars, String weakestKey) {
}
