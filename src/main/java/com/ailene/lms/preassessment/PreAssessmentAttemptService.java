package com.ailene.lms.preassessment;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreAssessmentAttemptService {

    private static final String SHUFFLED_SECTION = "scenario";
    private static final String PROFILE_SECTION = "profile";
    private static final String TEXT_TYPE = "text";
    private static final String SHADOW_AI_FLAG = "shadow_ai";

    private final AccessRepository accessRepository;
    private final PreAssessmentAttemptRepository attemptRepository;
    private final PreAssessmentAnswerGrader answerGrader;
    private final PreAssessmentScoreWriter scoreWriter;
    private final ObjectMapper objectMapper;

    public PreAssessmentQuestionsResponse getQuestions(UUID userId, PreAssessmentProjectRequest request) {
        Access access = findAccess(userId, request.projectId());
        PreAssessmentFormProjection form = attemptRepository.findActiveForm()
                .orElseThrow(() -> new ResourceNotFoundException("No active pre-assessment form"));

        Map<Integer, List<PreAssessmentOptionProjection>> optionsByItem = attemptRepository
                .findOptions(form.getId()).stream()
                .collect(Collectors.groupingBy(PreAssessmentOptionProjection::getItemId));

        List<PreAssessmentQuestionsResponse.Item> items = new ArrayList<>();
        for (PreAssessmentItemProjection item : attemptRepository.findItems(form.getId())) {
            List<PreAssessmentOptionProjection> options = optionsByItem.getOrDefault(item.getId(), List.of());
            if (SHUFFLED_SECTION.equals(item.getSection())) {
                options = shuffle(options, access.getId(), item.getId());
            }
            items.add(new PreAssessmentQuestionsResponse.Item(item.getId(), item.getItemCode(), item.getSection(),
                    item.getType(), item.getQuestion(), options.stream()
                            .map(o -> new PreAssessmentQuestionsResponse.Option(o.getId(), o.getText()))
                            .toList()));
        }

        PreAssessmentQuestionsResponse.Attempt attempt = attemptRepository.findByAccessId(access.getId())
                .map(PreAssessmentQuestionsResponse.Attempt::from)
                .orElse(null);
        return new PreAssessmentQuestionsResponse(PreAssessmentQuestionsResponse.Form.from(form), attempt, items);
    }

    @Transactional
    public PreAssessmentSubmitResponse submit(UUID userId, PreAssessmentSubmitRequest request) {
        Access access = findAccess(userId, request.projectId());
        PreAssessmentFormProjection form = attemptRepository.findForm(request.formId())
                .filter(f -> "active".equals(f.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment form not found"));

        if (attemptRepository.findByAccessId(access.getId()).isPresent()) {
            throw new BadRequestException("Pre-assessment already submitted.");
        }

        Map<Integer, PreAssessmentItemProjection> items = new LinkedHashMap<>();
        attemptRepository.findItems(form.getId()).forEach(i -> items.put(i.getId(), i));
        Map<Integer, PreAssessmentOptionProjection> options = new HashMap<>();
        attemptRepository.findOptions(form.getId()).forEach(o -> options.put(o.getId(), o));

        Map<Integer, PreAssessmentSubmitRequest.Answer> answers = new LinkedHashMap<>();
        for (PreAssessmentSubmitRequest.Answer answer : request.answers()) {
            PreAssessmentItemProjection item = items.get(answer.itemId());
            if (item == null) {
                throw new BadRequestException("Question " + answer.itemId() + " is not part of this form");
            }
            if (answers.put(answer.itemId(), answer) != null) {
                throw new BadRequestException("Question " + item.getItemCode() + " is answered more than once");
            }
            validateAnswer(item, answer, options);
        }

        List<String> missing = items.values().stream()
                .filter(i -> !answers.containsKey(i.getId()))
                .map(PreAssessmentItemProjection::getItemCode)
                .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("Every question must be answered, missing: " + String.join(", ", missing));
        }

        boolean shadowAi = answers.values().stream()
                .anyMatch(a -> a.optionId() != null && SHADOW_AI_FLAG.equals(options.get(a.optionId()).getFlag()));

        PreAssessmentAttempt attempt = new PreAssessmentAttempt();
        attempt.setAccessId(access.getId());
        attempt.setFormId(form.getId());
        attempt.setStatus(PreAssessmentAttemptStatus.submitted);
        attempt.setIsShadowAi(shadowAi);
        attempt.setSubmittedAt(OffsetDateTime.now());
        Integer attemptId = attemptRepository.save(attempt).getId();

        for (PreAssessmentSubmitRequest.Answer answer : answers.values()) {
            PreAssessmentOptionProjection option = answer.optionId() == null ? null : options.get(answer.optionId());
            attemptRepository.insertAnswer(attemptId, answer.itemId(), form.getId(), answer.optionId(),
                    answer.optionId() == null ? answer.text().strip() : null,
                    option == null ? null : option.getCredit());
        }

        return new PreAssessmentSubmitResponse(attemptId, attempt.getStatus());
    }

    // First call after submit grades the written answers with DeepSeek and computes the band; later calls only read.
    public PreAssessmentScoreResponse getScore(UUID userId, PreAssessmentProjectRequest request) {
        Access access = findAccess(userId, request.projectId());
        PreAssessmentAttempt attempt = attemptRepository.findByAccessId(access.getId()).orElse(null);
        if (attempt == null) {
            return null;
        }

        List<PreAssessmentItemProjection> items = attemptRepository.findItems(attempt.getFormId());
        if (attempt.getStatus() == PreAssessmentAttemptStatus.submitted) {
            scoreAttempt(attempt, items);
            attempt = attemptRepository.findById(attempt.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment attempt not found"));
        }
        return buildScoreResponse(attempt, items);
    }

    private void scoreAttempt(PreAssessmentAttempt attempt, List<PreAssessmentItemProjection> items) {
        Map<Integer, PreAssessmentAnswerProjection> answers = answersByItem(attempt.getId());

        List<PreAssessmentAnswerGrader.Task> tasks = new ArrayList<>();
        Map<String, Integer> itemIdByCode = new HashMap<>();
        StringBuilder context = new StringBuilder();
        for (PreAssessmentItemProjection item : items) {
            PreAssessmentAnswerProjection answer = answers.get(item.getId());
            if (answer == null || answer.getTextValue() == null) {
                continue;
            }
            if (PROFILE_SECTION.equals(item.getSection())) {
                context.append(item.getQuestion()).append('\n').append(answer.getTextValue()).append('\n');
            } else if (item.getGradingConfig() != null && !Boolean.TRUE.equals(answer.getIsGraded())) {
                tasks.add(new PreAssessmentAnswerGrader.Task(item.getItemCode(), item.getQuestion(),
                        readConfig(item.getGradingConfig()), answer.getTextValue()));
                itemIdByCode.put(item.getItemCode(), item.getId());
            }
        }
        if (!tasks.isEmpty()) {
            Map<Integer, PreAssessmentAnswerGrader.Grade> grades = new HashMap<>();
            answerGrader.grade(context.toString(), tasks).forEach((code, grade) -> grades.put(itemIdByCode.get(code), grade));
            scoreWriter.saveGrades(attempt.getId(), grades);
            answers = answersByItem(attempt.getId());
        }

        PreAssessmentConfigProjection config = attemptRepository.findConfig(null)
                .orElseThrow(() -> new ResourceNotFoundException("No active pre-assessment scoring config"));
        List<PreAssessmentBandEngine.Band> bands = attemptRepository.findBands(config.getId()).stream()
                .map(b -> new PreAssessmentBandEngine.Band(b.getBand(), b.getLevel(), b.getMinP1(), b.getMinP2(),
                        b.getMinP3()))
                .toList();

        PreAssessmentBandEngine.Result result = PreAssessmentBandEngine.score(
                items.stream().map(this::toEngineItem).toList(),
                answers.values().stream().collect(Collectors.toMap(PreAssessmentAnswerProjection::getItemId,
                        a -> new PreAssessmentBandEngine.Answer(a.getItemId(), a.getCredit(), a.getRubricTotal(),
                                Boolean.TRUE.equals(a.getHasSensitiveData())))),
                new PreAssessmentBandEngine.Config(config.getSecurityGateMaxBand(), config.getBehaviourGateFromBand(),
                        config.getBehaviourGateMaxBand(), config.getBehaviourGateMinScore().doubleValue(),
                        config.getBehaviourGateMinCredit().doubleValue(), config.getOverclaimMinCredit().doubleValue(),
                        config.getOverclaimCappedCredit().doubleValue()),
                bands);
        scoreWriter.saveResult(attempt.getId(), config.getId(), result);
    }

    private PreAssessmentScoreResponse buildScoreResponse(PreAssessmentAttempt attempt,
            List<PreAssessmentItemProjection> items) {
        PreAssessmentConfigProjection config = attemptRepository.findConfig(attempt.getConfigId())
                .orElseThrow(() -> new ResourceNotFoundException("Pre-assessment scoring config not found"));
        Map<Integer, PreAssessmentBandProjection> bands = attemptRepository.findBands(config.getId()).stream()
                .collect(Collectors.toMap(PreAssessmentBandProjection::getBand, Function.identity()));

        List<PreAssessmentDomainScoreProjection> domainScores = attemptRepository.findDomainScores(attempt.getId());
        Map<String, double[]> clusterTotals = new LinkedHashMap<>();
        Map<String, String> clusterNames = new LinkedHashMap<>();
        for (PreAssessmentDomainScoreProjection d : domainScores) {
            double[] totals = clusterTotals.computeIfAbsent(d.getClusterCode(), k -> new double[2]);
            totals[0] += d.getScore().doubleValue() * d.getItemCount();
            totals[1] += d.getItemCount();
            clusterNames.put(d.getClusterCode(), d.getClusterName());
        }
        List<PreAssessmentScoreResponse.Cluster> clusters = clusterTotals.entrySet().stream()
                .map(e -> new PreAssessmentScoreResponse.Cluster(e.getKey(), clusterNames.get(e.getKey()),
                        BigDecimal.valueOf(e.getValue()[0] / e.getValue()[1]).setScale(2, RoundingMode.HALF_UP)))
                .toList();
        List<PreAssessmentScoreResponse.Domain> domains = domainScores.stream()
                .map(d -> new PreAssessmentScoreResponse.Domain(d.getDomainCode(), d.getDomainName(),
                        d.getClusterCode(), d.getScore()))
                .toList();

        Map<Integer, String> codes = items.stream()
                .collect(Collectors.toMap(PreAssessmentItemProjection::getId, PreAssessmentItemProjection::getItemCode));
        Map<Integer, PreAssessmentAnswerProjection> answers = new TreeMap<>(answersByItem(attempt.getId()));
        List<PreAssessmentScoreResponse.Feedback> feedback = answers.values().stream()
                .filter(a -> a.getAiFeedback() != null)
                .map(a -> new PreAssessmentScoreResponse.Feedback(codes.get(a.getItemId()), a.getAiFeedback()))
                .toList();
        boolean sensitiveData = answers.values().stream().anyMatch(a -> Boolean.TRUE.equals(a.getHasSensitiveData()));

        int finalBand = attempt.getFinalBand();
        int targetBand = config.getTargetBand();
        return new PreAssessmentScoreResponse(attempt.getId(), attempt.getStatus(), attempt.getSubmittedAt(),
                attempt.getScoredAt(), band(bands, finalBand), band(bands, attempt.getRawBand()),
                band(bands, targetBand), Math.max(0, targetBand - finalBand), finalBand >= targetBand,
                new PreAssessmentScoreResponse.Dimensions(attempt.getKnowledgeScore(), attempt.getSkillsScore(),
                        attempt.getBehavioursScore()),
                new PreAssessmentScoreResponse.LevelMastery(attempt.getP1(), attempt.getP2(), attempt.getP3()),
                new PreAssessmentScoreResponse.Gates(attempt.getSecurityGatePassed(), attempt.getBehaviourGatePassed()),
                new PreAssessmentScoreResponse.Flags(attempt.getIsOverclaim(), attempt.getIsShadowAi(), sensitiveData),
                clusters, domains, feedback);
    }

    private PreAssessmentBandEngine.Item toEngineItem(PreAssessmentItemProjection item) {
        Map<Integer, Integer> thresholds = new TreeMap<>();
        if (item.getGradingConfig() != null
                && readConfig(item.getGradingConfig()).get("level_thresholds") instanceof Map<?, ?> raw) {
            raw.forEach((level, min) -> thresholds.put(Integer.valueOf(level.toString()), ((Number) min).intValue()));
        }
        return new PreAssessmentBandEngine.Item(item.getId(), item.getSection(), item.getType(), item.getDomainId(),
                item.getDimension(), item.getCfteLevel(), Boolean.TRUE.equals(item.getIsCritical()),
                Boolean.TRUE.equals(item.getIsBehaviourGate()), item.getOverclaimItemId(), thresholds);
    }

    private static PreAssessmentScoreResponse.Band band(Map<Integer, PreAssessmentBandProjection> bands, int band) {
        PreAssessmentBandProjection b = bands.get(band);
        return b == null ? new PreAssessmentScoreResponse.Band(band, null, null)
                : new PreAssessmentScoreResponse.Band(band, b.getName(), b.getLevel());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readConfig(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    private Map<Integer, PreAssessmentAnswerProjection> answersByItem(Integer attemptId) {
        return attemptRepository.findAnswers(attemptId).stream()
                .collect(Collectors.toMap(PreAssessmentAnswerProjection::getItemId, Function.identity()));
    }

    private Access findAccess(UUID userId, String projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }

    private static void validateAnswer(PreAssessmentItemProjection item, PreAssessmentSubmitRequest.Answer answer,
            Map<Integer, PreAssessmentOptionProjection> options) {
        if (TEXT_TYPE.equals(item.getType())) {
            if (answer.optionId() != null || answer.text() == null || answer.text().isBlank()) {
                throw new BadRequestException("Question " + item.getItemCode() + " needs a text answer");
            }
            return;
        }
        PreAssessmentOptionProjection option = answer.optionId() == null ? null : options.get(answer.optionId());
        if (option == null || !option.getItemId().equals(item.getId()) || answer.text() != null) {
            throw new BadRequestException("Question " + item.getItemCode() + " needs one of its own options");
        }
    }

    // Seeded per access and item so a reload shows the same order; "Saya tidak tahu" always stays last.
    private static List<PreAssessmentOptionProjection> shuffle(List<PreAssessmentOptionProjection> options,
            String accessId, Integer itemId) {
        List<PreAssessmentOptionProjection> shuffled = new ArrayList<>(options.stream()
                .filter(o -> !Boolean.TRUE.equals(o.getIsDontKnow())).toList());
        Collections.shuffle(shuffled, new Random((accessId + ":" + itemId).hashCode()));
        options.stream().filter(o -> Boolean.TRUE.equals(o.getIsDontKnow())).forEach(shuffled::add);
        return shuffled;
    }
}
