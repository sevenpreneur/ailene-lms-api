package com.ailene.lms.admin;

import com.ailene.lms.preassessment.PreAssessmentBaseline;

import java.time.Instant;
import java.util.List;

public record AdminPreAssessmentInsights(Summary summary, PreAssessmentBaseline.Band targetBand,
        List<PreAssessmentBaseline.BandCount> bands, List<PreAssessmentBaseline.Cluster> clusters,
        List<GroupRow> groups, List<Member> members) {

    public record Summary(int total, int submitted, int scored, int meetsTarget, Double averageScore,
            int flaggedSecurity, int flaggedOverclaim, int flaggedShadowAi) {
    }

    public record GroupRow(Integer groupId, String groupName, int total, int scored, int meetsTarget,
            Double averageScore) {
    }

    public record Member(String accessId, String fullName, String avatar, Integer groupId, String groupName,
            String status, PreAssessmentBaseline.Band band, Double averageScore, boolean meetsTarget,
            Instant submittedAt, List<PreAssessmentBaseline.Cluster> clusters, Flags flags) {
    }

    public record Flags(Double knowledgeScore, Double skillsScore, Double behavioursScore, Boolean securityGatePassed,
            Boolean behaviourGatePassed, Boolean isOverclaim, Boolean isShadowAi) {
    }
}
