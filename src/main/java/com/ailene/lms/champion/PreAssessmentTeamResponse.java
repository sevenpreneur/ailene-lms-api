package com.ailene.lms.champion;

import com.ailene.lms.preassessment.PreAssessmentBaseline;

import java.time.Instant;
import java.util.List;

public record PreAssessmentTeamResponse(Integer groupId, String groupName, int totalMembers, int submittedCount,
        int scoredCount, Instant measuredAt, PreAssessmentBaseline.Band targetBand, int meetsTargetCount,
        Double teamAvg, List<PreAssessmentBaseline.Cluster> teamClusters,
        List<PreAssessmentBaseline.BandCount> bands, List<TeamMemberBaseline> members) {
}
