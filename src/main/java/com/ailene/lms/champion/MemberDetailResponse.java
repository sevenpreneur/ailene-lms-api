package com.ailene.lms.champion;

import java.util.List;

public record MemberDetailResponse(MemberDetailInfo member, MemberDetailMetrics metrics, MemberRadar radar,
        MemberGate gate, List<MemberActivity> activities, List<MemberNote> notes) {
}
