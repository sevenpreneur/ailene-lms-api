package com.ailene.lms.preassessment;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PreAssessmentBaselineReader {

    private final PreAssessmentBaselineRepository baselineRepository;
    private final PreAssessmentAttemptRepository attemptRepository;

    // A null groupId reads the whole project; band names and the target come from the active config.
    public PreAssessmentBaseline read(String projectId, Integer groupId) {
        PreAssessmentConfigProjection config = attemptRepository.findConfig(null)
                .orElseThrow(() -> new ResourceNotFoundException("No active pre-assessment scoring config"));
        Map<Integer, PreAssessmentBaseline.Band> bands = new LinkedHashMap<>();
        for (PreAssessmentBandProjection b : attemptRepository.findBands(config.getId())) {
            bands.put(b.getBand(), new PreAssessmentBaseline.Band(b.getBand(), b.getName(), b.getLevel()));
        }

        List<PreAssessmentBaseline.ClusterRef> clusters = baselineRepository.findClusters().stream()
                .map(c -> new PreAssessmentBaseline.ClusterRef(c.getCode(), c.getName()))
                .toList();

        Map<String, Map<String, Double>> scores = new HashMap<>();
        for (PreAssessmentBaselineClusterScoreProjection row : baselineRepository.findClusterScores(projectId,
                groupId)) {
            if (row.getScore() != null) {
                scores.computeIfAbsent(row.getAccessId(), key -> new HashMap<>())
                        .put(row.getClusterCode(), row.getScore().doubleValue());
            }
        }

        List<PreAssessmentBaseline.Member> members = baselineRepository.findMembers(projectId, groupId).stream()
                .map(row -> new PreAssessmentBaseline.Member(row.getAccessId(), row.getGroupId(),
                        row.getGroupName(), row.getFullName(), row.getAvatar(),
                        row.getStatus() == null ? PreAssessmentBaseline.NOT_STARTED : row.getStatus(),
                        row.getBand(), row.getSubmittedAt(), scores.getOrDefault(row.getAccessId(), Map.of())))
                .toList();

        int target = config.getTargetBand();
        return new PreAssessmentBaseline(members, clusters, bands,
                bands.getOrDefault(target, new PreAssessmentBaseline.Band(target, null, null)));
    }
}
