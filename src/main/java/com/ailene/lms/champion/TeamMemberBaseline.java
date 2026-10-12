package com.ailene.lms.champion;

import com.ailene.lms.preassessment.PreAssessmentBaseline;

import java.util.List;

public record TeamMemberBaseline(String accessId, String name, String avatar, String status,
        PreAssessmentBaseline.Band band, Double avg, boolean meetsTarget,
        List<PreAssessmentBaseline.Cluster> clusters, String weakestCode, boolean isMe) {
}
