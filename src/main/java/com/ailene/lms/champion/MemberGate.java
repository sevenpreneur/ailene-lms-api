package com.ailene.lms.champion;

import java.util.List;

public record MemberGate(short fromLevel, short toLevel, Integer nextLevelId, int done, int total, int percent,
        boolean ready, List<GateRequirement> requirements) {
}
