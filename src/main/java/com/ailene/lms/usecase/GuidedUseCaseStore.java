package com.ailene.lms.usecase;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.common.NanoId;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.*;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.notification.NotificationWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuidedUseCaseStore {
    public static final int MAX_GENERATIONS = 3;
    private final GuidedSessionRepository sessions;
    private final GuidedGenerationRepository generations;
    private final UseCaseRepository useCases;
    private final UseCaseSubmissionRepository submissions;
    private final AccessRepository accesses;
    private final LevelRepository levels;
    private final NotificationWriter notifications;
    private final GuidedJson json;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public GuidedSessionResponse details(UUID userId, GuidedDetailsRequest request) {
        if (request.sessionId() != null) return view(owned(userId, request.sessionId()));
        String projectId = project(request.projectId(), request.useCaseId());
        Access access = student(userId, projectId, false);
        GuidedSession session = request.useCaseId() == null
                ? sessions.findFirstByStudentAccessIdAndProjectIdAndUseCaseIdIsNullAndStatusNotOrderByIdDesc(
                        access.getId(), projectId, GuidedStatus.submitted).orElse(null)
                : sessions.findFirstByStudentAccessIdAndUseCaseIdOrderByIdDesc(
                        access.getId(), request.useCaseId()).orElse(null);
        return session == null ? null : view(session);
    }

    @Transactional
    public GuidedSessionResponse save(UUID userId, GuidedSaveRequest r) {
        GuidedSession s;
        if (r.sessionId() != null) {
            s = lockOwned(userId, r.sessionId());
            if ((r.projectId() != null && !r.projectId().equals(s.getProjectId()))
                    || (r.useCaseId() != null && !r.useCaseId().equals(s.getUseCaseId()))) {
                throw new BadRequestException("Sesi tidak sesuai dengan project atau use case.");
            }
        } else {
            String projectId = project(r.projectId(), r.useCaseId());
            Access access = student(userId, projectId, true);
            s = r.useCaseId() == null
                    ? sessions.findFirstByStudentAccessIdAndProjectIdAndUseCaseIdIsNullAndStatusNotOrderByIdDesc(
                            access.getId(), projectId, GuidedStatus.submitted).orElse(null)
                    : sessions.findFirstByStudentAccessIdAndUseCaseIdOrderByIdDesc(
                            access.getId(), r.useCaseId()).orElse(null);
            if (s == null) {
                s = new GuidedSession();
                s.setProjectId(projectId);
                s.setStudentAccessId(access.getId());
                s.setUseCaseId(r.useCaseId());
                sessions.saveAndFlush(s);
                entityManager.refresh(s);
            } else {
                // The lookup may predate a concurrent save; refresh under lock before applying this patch.
                entityManager.refresh(s, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            }
        }
        mutable(s);
        idle(s);
        String before = intakeSnapshot(s);
        if (r.dailyTask() != null) s.setDailyTask(GuidedRules.clean(r.dailyTask()));
        if (r.frequency() != null) s.setFrequency(r.frequency());
        boolean exampleChanged = (r.exampleText() != null && !Objects.equals(
                GuidedRules.clean(r.exampleText()), s.getExampleText()))
                || (r.exampleFileUrl() != null && !Objects.equals(
                        GuidedRules.clean(r.exampleFileUrl()), s.getExampleFileUrl()));
        if (r.exampleText() != null) s.setExampleText(GuidedRules.clean(r.exampleText()));
        if (r.exampleFileUrl() != null) {
            String url = GuidedRules.clean(r.exampleFileUrl());
            if (url != null) GuidedRules.requireLink(url, true);
            s.setExampleFileUrl(url);
        }
        if (exampleChanged) s.setSensitiveDataConfirmed(false);
        if (r.sensitiveDataConfirmed() != null) s.setSensitiveDataConfirmed(r.sensitiveDataConfirmed());
        if (r.minutesWithoutAi() != null) s.setMinutesWithoutAi(r.minutesWithoutAi());
        if (r.currentProcess() != null) s.setCurrentProcess(GuidedRules.clean(r.currentProcess()));
        if (r.currentTools() != null) s.setCurrentToolsJson(json.write(r.currentTools().stream().map(String::strip).distinct().toList()));
        if (r.painPoint() != null) s.setPainPoint(GuidedRules.clean(r.painPoint()));
        GuidedRules.requireSafe(s.getDailyTask(), s.getExampleText(), s.getCurrentProcess(), s.getPainPoint(),
                String.join(", ", json.strings(s.getCurrentToolsJson())));
        if (!before.equals(intakeSnapshot(s))) {
            if (s.getGenerationsUsed() >= MAX_GENERATIONS && !json.steps(s.getStepsJson()).isEmpty()) {
                throw new ConflictException("Kuota generate sudah habis. Lanjutkan dengan workflow yang tersedia.");
            }
            s.setStepsJson("[]");
            s.setSummary(null);
            s.setRisksJson("[]");
            s.setEstimatedMinutesWithAi(null);
            s.setStatus(GuidedStatus.intake);
        }
        List<GuidedStep> steps = json.steps(s.getStepsJson());
        GuidedRules.requireThrough(s, steps, r.currentStep());
        s.setCurrentStep(r.currentStep());
        if (r.currentStep() >= 5) {
            s.setStatus(GuidedRules.completed(steps) ? GuidedStatus.ready_to_submit : GuidedStatus.in_progress);
        }
        touch(s);
        return view(s);
    }

    // Runs before anything is written, so a rejected champion prefill leaves no use case behind.
    public void requirePrefill(GuidedPrefill p) {
        GuidedSession s = new GuidedSession();
        applyPrefill(s, p);
        GuidedRules.requireThrough(s, List.of(), 4);
    }

    // A champion-prefilled session opens on the workflow step; the student still generates, runs and submits it.
    @Transactional
    public void prefill(String studentAccessId, String projectId, Integer useCaseId, GuidedPrefill p) {
        GuidedSession s = new GuidedSession();
        s.setStudentAccessId(studentAccessId);
        s.setProjectId(projectId);
        s.setUseCaseId(useCaseId);
        applyPrefill(s, p);
        GuidedRules.requireThrough(s, List.of(), 4);
        s.setCurrentStep(4);
        sessions.save(s);
    }

    private void applyPrefill(GuidedSession s, GuidedPrefill p) {
        s.setDailyTask(GuidedRules.clean(p.dailyTask()));
        s.setFrequency(p.frequency());
        s.setExampleText(GuidedRules.clean(p.exampleText()));
        String url = GuidedRules.clean(p.exampleFileUrl());
        if (url != null) GuidedRules.requireLink(url, true);
        s.setExampleFileUrl(url);
        s.setSensitiveDataConfirmed(Boolean.TRUE.equals(p.sensitiveDataConfirmed()));
        s.setMinutesWithoutAi(p.minutesWithoutAi());
        s.setCurrentProcess(GuidedRules.clean(p.currentProcess()));
        List<String> tools = p.currentTools() == null ? List.of() : p.currentTools().stream().map(String::strip).distinct().toList();
        s.setCurrentToolsJson(json.write(tools));
        s.setPainPoint(GuidedRules.clean(p.painPoint()));
        GuidedRules.requireSafe(s.getDailyTask(), s.getExampleText(), s.getCurrentProcess(), s.getPainPoint(),
                String.join(", ", tools));
    }

    @Transactional
    public GenerationReservation reserve(UUID userId, GuidedGenerateRequest r) {
        GuidedSession s = lockOwned(userId, r.sessionId());
        mutable(s);
        idle(s);
        GuidedRules.requireThrough(s, json.steps(s.getStepsJson()), 4);
        if (GuidedRules.clean(s.getExampleText()) == null) {
            throw new BadRequestException("Tambahkan teks atau ringkasan isi file yang sudah disamarkan sebelum membuat workflow.");
        }
        GuidedRules.requireSafe(s.getDailyTask(), s.getExampleText(), s.getCurrentProcess(), s.getPainPoint(), r.feedback());
        if (s.getGenerationsUsed() >= MAX_GENERATIONS) {
            throw new TooManyRequestsException("Kuota generate workflow untuk sesi ini sudah habis.");
        }
        s.setGenerationToken(NanoId.generate());
        s.setGenerationStartedAt(OffsetDateTime.now());
        s.setGenerationsUsed(s.getGenerationsUsed() + 1);
        touch(s);
        return new GenerationReservation(s, s.getGenerationToken());
    }

    @Transactional
    public GuidedSessionResponse finish(UUID userId, GenerationReservation reserved, String feedback,
            GuidedWorkflow workflow) {
        GuidedSession s = lockOwned(userId, reserved.session().getId());
        if (!Objects.equals(s.getGenerationToken(), reserved.token())) {
            throw new ConflictException("Request generate sudah kedaluwarsa. Muat ulang sesi.");
        }
        GuidedGeneration generation = new GuidedGeneration();
        generation.setSessionId(s.getId());
        generation.setVersion(s.getGenerationsUsed());
        generation.setFeedback(GuidedRules.clean(feedback));
        generation.setWorkflow(json.write(workflow));
        generations.save(generation);
        s.setStepsJson(json.write(workflow.steps()));
        s.setRisksJson(json.write(workflow.risks()));
        s.setSummary(workflow.summary());
        s.setEstimatedMinutesWithAi(workflow.estimatedMinutesWithAi());
        s.setGenerationToken(null);
        s.setGenerationStartedAt(null);
        s.setCurrentStep(4);
        s.setStatus(GuidedStatus.steps_ready);
        touch(s);
        return view(s);
    }

    @Transactional
    public void release(UUID userId, GenerationReservation reserved) {
        GuidedSession s = lockOwned(userId, reserved.session().getId());
        if (Objects.equals(s.getGenerationToken(), reserved.token())) {
            s.setGenerationToken(null);
            s.setGenerationStartedAt(null);
            s.setGenerationsUsed(s.getGenerationsUsed() - 1);
            touch(s);
        }
    }

    @Transactional
    public GuidedStep check(UUID userId, GuidedCheckRequest r) {
        GuidedSession s = lockOwned(userId, r.sessionId());
        mutable(s);
        idle(s);
        if (s.getStatus() != GuidedStatus.in_progress && s.getStatus() != GuidedStatus.ready_to_submit) {
            throw new BadRequestException("Konfirmasi workflow sebelum mengisi checklist.");
        }
        GuidedRules.requireSafe(r.note(), r.skippedReason());
        List<GuidedStep> steps = new ArrayList<>(json.steps(s.getStepsJson()));
        GuidedStep old = steps.stream().filter(step -> step.id().equals(r.stepId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Langkah tidak ditemukan pada workflow terbaru."));
        GuidedStep updated = new GuidedStep(old.id(), old.order(), old.title(), old.instruction(),
                old.suggestedTool(), old.promptTemplate(), old.estimatedMinutes(), old.verification(), r.isDone(),
                r.isDone() ? null : r.skippedReason() == null ? old.skippedReason() : GuidedRules.clean(r.skippedReason()),
                r.note() == null ? old.note() : GuidedRules.clean(r.note()),
                r.isDone() ? old.isDone() ? old.doneAt() : OffsetDateTime.now() : null);
        steps.set(steps.indexOf(old), updated);
        s.setStepsJson(json.write(steps));
        s.setStatus(GuidedRules.completed(steps) ? GuidedStatus.ready_to_submit : GuidedStatus.in_progress);
        if (!GuidedRules.completed(steps) && s.getCurrentStep() > 5) s.setCurrentStep(5);
        touch(s);
        return updated;
    }

    @Transactional
    public GuidedSubmitResponse submit(UUID userId, GuidedSubmitRequest r) {
        return submitLocked(userId, r, null);
    }

    @Transactional
    public GuidedSubmitResponse submitExisting(UUID userId, UseCaseSubmitRequest r) {
        lockOwned(userId, r.guidedSessionId());
        UseCaseSubmission existing = submissions.findByGuidedSessionId(r.guidedSessionId()).orElse(null);
        GuidedSubmitRequest guided = new GuidedSubmitRequest(r.guidedSessionId(), null, null,
                r.outcomeProof(), r.hoursWithAi(), r.description(), r.aiTool(), r.type(),
                r.testimonial() != null ? r.testimonial() : existing == null ? null : existing.getTestimonial(),
                r.helpfulnessRating() != null ? r.helpfulnessRating() : existing == null ? null : existing.getHelpfulnessRating(),
                r.shareConsent() != null ? r.shareConsent() : existing == null ? null : existing.getShareConsent());
        return submitLocked(userId, guided, r.useCaseId());
    }

    private GuidedSubmitResponse submitLocked(UUID userId, GuidedSubmitRequest r, Integer expectedUseCaseId) {
        GuidedSession s = lockOwned(userId, r.sessionId());
        idle(s);
        if (expectedUseCaseId != null && !expectedUseCaseId.equals(s.getUseCaseId())) {
            throw new BadRequestException("Sesi panduan tidak sesuai dengan use case.");
        }
        UseCaseSubmission submission = submissions.findByGuidedSessionId(s.getId()).orElse(null);
        if (s.getStatus() == GuidedStatus.submitted && submission != null && !needsRevision(submission)) {
            return result(s, submission);
        }
        GuidedRules.requireOutcome(r);
        GuidedRules.requireThrough(s, json.steps(s.getStepsJson()), 6);
        if (s.getStatus() != GuidedStatus.ready_to_submit && s.getStatus() != GuidedStatus.submitted) {
            throw new BadRequestException("Selesaikan checklist terlebih dahulu.");
        }
        if (s.getUseCaseId() == null) {
            GroupSummaryProjection group = accesses.findGroupSummary(userId, s.getProjectId()).orElseThrow(this::notFound);
            String champion = champion(s, group);
            List<Short> categories = r.categoryIds() == null ? List.of() : r.categoryIds().stream().distinct().toList();
            if (!categories.isEmpty() && useCases.countExistingCategories(categories) != categories.size()) {
                throw new ResourceNotFoundException("Kategori tidak ditemukan.");
            }
            Level level = levels.findByLevelNumber((short) 3)
                    .orElseThrow(() -> new ResourceNotFoundException("Level use case tidak ditemukan."));
            UseCase useCase = new UseCase();
            useCase.setOnlyProjectId(s.getProjectId());
            useCase.setLevelId(level.getId());
            String name = GuidedRules.clean(r.name());
            if (name == null) name = s.getDailyTask().substring(0, Math.min(255, s.getDailyTask().length()));
            GuidedRules.requireSafe(name);
            useCase.setName(name);
            useCase.setDescription(r.description().strip());
            useCase.setStatus(Status.active);
            useCase.setIsSelfCreated(true);
            useCases.saveAndFlush(useCase);
            for (Short category : categories) useCases.insertCategory(useCase.getId(), category);
            s.setUseCaseId(useCase.getId());
            submission = new UseCaseSubmission();
            submission.setUseCaseId(useCase.getId());
            submission.setStudentAccessId(s.getStudentAccessId());
            submission.setAssignedByAccessId(champion);
        } else if (submission == null) {
            useCases.findById(s.getUseCaseId()).filter(u -> u.getStatus() == Status.active)
                    .orElseThrow(() -> new ResourceNotFoundException("Use case tidak ditemukan."));
            submission = submissions.findByStudentAccessIdAndUseCaseId(s.getStudentAccessId(), s.getUseCaseId())
                    .orElseGet(UseCaseSubmission::new);
            if (Boolean.TRUE.equals(submission.getIsAccepted())) throw new ConflictException("Use case sudah diterima.");
            if (submission.getGuidedSessionId() != null && !submission.getGuidedSessionId().equals(s.getId())) {
                throw new ConflictException("Submission sudah terhubung dengan sesi lain.");
            }
            submission.setUseCaseId(s.getUseCaseId());
            submission.setStudentAccessId(s.getStudentAccessId());
            if (submission.getAssignedByAccessId() == null) {
                submission.setAssignedByAccessId(champion(s,
                        accesses.findGroupSummary(userId, s.getProjectId()).orElseThrow(this::notFound)));
            }
        }
        submission.setOutcomeProof(r.outcomeProof().strip());
        submission.setHoursWithAi(r.hoursWithAi());
        submission.setHoursWithoutAi(GuidedRules.hoursWithoutAi(s));
        submission.setHoursSaved(GuidedRules.hoursSaved(s, r.hoursWithAi()));
        submission.setDescription(r.description().strip());
        submission.setAiTool(r.aiTool().strip());
        submission.setFrequency(s.getFrequency());
        submission.setType(r.type());
        submission.setGuidedSessionId(s.getId());
        submission.setTestimonial(r.testimonial().strip());
        submission.setHelpfulnessRating(r.helpfulnessRating());
        submission.setShareConsent(r.shareConsent());
        submission.setSubmittedAt(OffsetDateTime.now());
        submissions.saveAndFlush(submission);
        s.setStatus(GuidedStatus.submitted);
        s.setCurrentStep(6);
        touch(s);
        String name = useCases.findById(s.getUseCaseId()).orElseThrow(this::notFound).getName();
        notifications.useCaseSubmitted(submission.getAssignedByAccessId(), submission.getId(), s.getStudentAccessId(), name);
        return result(s, submission);
    }

    @Transactional(readOnly = true)
    public GuidedSessionResponse forReview(Integer sessionId) {
        return view(sessions.findById(sessionId).orElseThrow(this::notFound));
    }

    private GuidedSubmitResponse result(GuidedSession s, UseCaseSubmission submission) {
        String status = Boolean.TRUE.equals(submission.getIsAccepted()) ? "accepted"
                : needsRevision(submission) ? "needs_revision" : "awaiting_review";
        return new GuidedSubmitResponse(view(s), submission.getId(), s.getUseCaseId(), status, submission.getHoursSaved());
    }

    private boolean needsRevision(UseCaseSubmission submission) {
        return !Boolean.TRUE.equals(submission.getIsAccepted()) && submission.getReviewedAt() != null
                && submission.getSubmittedAt() != null && submission.getReviewedAt().isAfter(submission.getSubmittedAt());
    }

    private String champion(GuidedSession s, GroupSummaryProjection group) {
        if (group.getGroupId() == null) throw new BadRequestException("Kamu belum tergabung dalam grup.");
        return accesses.findChampionAccessId(s.getProjectId(), group.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Champion grup belum tersedia."));
    }

    private GuidedSessionResponse view(GuidedSession s) {
        return GuidedSessionResponse.from(s, json);
    }

    private GuidedSession owned(UUID userId, Integer id) {
        return sessions.findOwned(id, userId, AccessRole.student).orElseThrow(this::notFound);
    }

    private GuidedSession lockOwned(UUID userId, Integer id) {
        return sessions.lockOwned(id, userId, AccessRole.student).orElseThrow(this::notFound);
    }

    private Access student(UUID userId, String projectId, boolean lock) {
        Access access = (lock ? accesses.lockByUserIdAndProjectId(userId, projectId)
                : accesses.findByUserIdAndProjectId(userId, projectId)).orElseThrow(this::notFound);
        if (access.getRole() != AccessRole.student) throw new ForbiddenException("Fitur ini hanya untuk student.");
        return access;
    }

    private String project(String requested, Integer useCaseId) {
        String projectId = GuidedRules.clean(requested);
        if (useCaseId != null) {
            UseCase useCase = useCases.findById(useCaseId).filter(u -> u.getStatus() == Status.active)
                    .orElseThrow(() -> new ResourceNotFoundException("Use case tidak ditemukan."));
            if (projectId == null) projectId = useCase.getOnlyProjectId();
            if (useCase.getOnlyProjectId() != null && !useCase.getOnlyProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("Use case tidak ditemukan.");
            }
        }
        if (projectId == null) throw new BadRequestException("project_id wajib untuk pekerjaan baru atau library bersama.");
        return projectId;
    }

    private String intakeSnapshot(GuidedSession s) {
        return json.write(java.util.Arrays.asList(s.getDailyTask(), s.getFrequency(), s.getExampleText(),
                s.getExampleFileUrl(), s.isSensitiveDataConfirmed(), s.getMinutesWithoutAi(),
                s.getCurrentProcess(), s.getCurrentToolsJson(), s.getPainPoint()));
    }

    private void mutable(GuidedSession s) {
        if (s.getStatus() == GuidedStatus.submitted) throw new ConflictException("Sesi sudah dikirim dan tidak bisa diubah.");
    }

    private void idle(GuidedSession s) {
        if (GuidedRules.generating(s)) {
            throw new ConflictException("Workflow sedang dibuat. Tunggu hingga proses selesai.");
        }
        s.setGenerationToken(null);
        s.setGenerationStartedAt(null);
    }

    private void touch(GuidedSession s) {
        s.setUpdatedAt(OffsetDateTime.now());
        sessions.saveAndFlush(s);
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Sesi atau akses project tidak ditemukan.");
    }

    public record GenerationReservation(GuidedSession session, String token) {
    }
}
