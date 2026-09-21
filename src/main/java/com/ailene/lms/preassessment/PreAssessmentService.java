package com.ailene.lms.preassessment;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.qstash.QStashClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreAssessmentService {

    private static final String REPORT_JOB_PATH = "/api/v1/pre-assessment/report-callback";
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final PreAssessmentRepository preAssessmentRepository;
    private final PreAssessmentReportRepository preAssessmentReportRepository;
    private final PreAssessmentRecommendationGenerator recommendationGenerator;
    private final QStashClient qStashClient;

    @Transactional
    public PreAssessmentCreateResponse create(UUID userId, PreAssessmentCreateRequest request) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        if (preAssessmentRepository.findByAccessId(access.getId()).isPresent()) {
            throw new BadRequestException("Pre-assessment already submitted.");
        }

        PreAssessment preAssessment = new PreAssessment();
        preAssessment.setAccessId(access.getId());
        preAssessment.setAiUseFrequency(request.aiUseFrequency());
        preAssessment.setAiToolsUsed(toArray(request.aiToolsUsed()));
        preAssessment.setAiLimitations(toArray(request.aiLimitations()));
        preAssessment.setOutputReview(request.outputReview());
        preAssessment.setUseCases(toArray(request.useCases()));
        preAssessment.setTeamAdoption(request.teamAdoption());
        preAssessment.setConcreteExample(request.concreteExample());
        preAssessment.setModelSelection(request.modelSelection());
        preAssessment.setMultimodalUse(request.multimodalUse());
        preAssessment.setWorkflowReuse(request.workflowReuse());
        preAssessment.setPromptComfort(request.promptComfort());
        preAssessment.setPromptIteration(request.promptIteration());
        preAssessment.setRefineScenario(request.refineScenario());
        preAssessment.setProfessionalAttitude(request.professionalAttitude());
        preAssessment.setDataSafetyCheck(request.dataSafetyCheck());
        preAssessment.setPublishUnchecked(request.publishUnchecked());
        preAssessment.setBiggestChallenge(request.biggestChallenge());
        preAssessment.setTrainingExpectation(request.trainingExpectation());
        preAssessment.setMotivation(request.motivation());
        Integer preAssessmentId = preAssessmentRepository.save(preAssessment).getId();

        PreAssessmentReport report = new PreAssessmentReport();
        report.setPreAssessmentId(preAssessmentId);
        preAssessmentReportRepository.save(report);

        qStashClient.publish(REPORT_JOB_PATH, new PreAssessmentReportJobRequest(preAssessmentId));

        return new PreAssessmentCreateResponse(preAssessmentId);
    }

    public PreAssessmentScoreResponse getScore(UUID userId, PreAssessmentProjectRequest request) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        PreAssessment preAssessment = preAssessmentRepository.findByAccessId(access.getId()).orElse(null);
        if (preAssessment == null) {
            return new PreAssessmentScoreResponse(null, null);
        }

        return new PreAssessmentScoreResponse(PreAssessmentAnswers.from(preAssessment),
                PreAssessmentReportBuilder.build(preAssessment));
    }

    public PreAssessmentRecommendationsResponse getRecommendations(UUID userId, PreAssessmentProjectRequest request) {
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        PreAssessment preAssessment = preAssessmentRepository.findByAccessId(access.getId()).orElse(null);
        PreAssessmentReport report = preAssessment == null ? null
                : preAssessmentReportRepository.findByPreAssessmentId(preAssessment.getId()).orElse(null);

        if (report == null) {
            return new PreAssessmentRecommendationsResponse(PreAssessmentReportStatus.pending, null, null, null);
        }

        return new PreAssessmentRecommendationsResponse(report.getStatus(), report.getRecommendations(),
                report.getErrorMessage(), report.getGeneratedAt());
    }

    // No enclosing @Transactional: each status write below must commit independently of the eventual rethrow.
    public void generateReport(Integer preAssessmentId) {
        PreAssessmentReport report = preAssessmentReportRepository.findByPreAssessmentId(preAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment report not found"));
        report.setStatus(PreAssessmentReportStatus.processing);
        report.setErrorMessage(null);
        preAssessmentReportRepository.save(report);

        try {
            PreAssessment preAssessment = preAssessmentRepository.findById(preAssessmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment not found"));
            Access access = accessRepository.findById(preAssessment.getAccessId())
                    .orElseThrow(() -> new ResourceNotFoundException("Access not found"));
            List<String> lessons = chapterRepository.findActiveChapterNames(access.getProjectId(), access.getId());

            var recommendations = recommendationGenerator.generate(preAssessment, lessons);

            report.setStatus(PreAssessmentReportStatus.completed);
            report.setRecommendations(recommendations);
            report.setErrorMessage(null);
            report.setGeneratedAt(OffsetDateTime.now());
            preAssessmentReportRepository.save(report);
        } catch (RuntimeException e) {
            report.setStatus(PreAssessmentReportStatus.failed);
            report.setErrorMessage(truncate(e.getMessage()));
            preAssessmentReportRepository.save(report);
            throw e;
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "generation_failed";
        }
        return message.length() > ERROR_MESSAGE_MAX_LENGTH ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH)
                : message;
    }

    private static String[] toArray(List<String> values) {
        return values.toArray(new String[0]);
    }
}
