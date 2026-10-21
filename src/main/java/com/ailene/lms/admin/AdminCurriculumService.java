package com.ailene.lms.admin;

import com.ailene.lms.admin.AdminCurriculumDto.ContentKind;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.notification.NotificationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCurriculumService {
    private final AdminGuard guard;
    private final AdminCurriculumRepository repository;
    private final NotificationWriter notifications;

    @Transactional(readOnly = true)
    public AdminCurriculumDto list(String token, AdminProjectRequest r) {
        guard.requireAdminOnProject(token, r.projectId());
        return repository.list(r.projectId());
    }

    @Transactional
    public IdResponse saveChapter(String token, AdminCurriculumRequest.Chapter r, boolean creating) {
        guard.requireAdminOnProject(token, r.projectId());
        if (!repository.levelExists(r.levelId())) throw new BadRequestException("Unknown learning level");
        if (creating) return new IdResponse(repository.createChapter(r));
        if (r.chapterId() == null) throw new BadRequestException("chapter_id is required");
        repository.lockChapter(r.projectId(), r.chapterId());
        repository.updateChapter(r);
        return new IdResponse(r.chapterId());
    }

    @Transactional
    public AdminDeleteResponse deleteChapter(String token, AdminCurriculumRequest.ChapterRef r) {
        authorize(token, r.projectId(), r.chapterId());
        for (ContentKind kind : ContentKind.values()) {
            String id = repository.contentId(r.chapterId(), kind);
            if (id != null && repository.hasProgress(kind, id)) throw inUse();
        }
        try {
            for (ContentKind kind : ContentKind.values()) {
                String id = repository.contentId(r.chapterId(), kind);
                if (id != null) repository.deleteContent(kind, id);
            }
            repository.deleteChapter(r.projectId(), r.chapterId());
        } catch (DataIntegrityViolationException e) { throw inUse(); }
        return new AdminDeleteResponse(true);
    }

    @Transactional
    public IdResponse saveSession(String token, AdminCurriculumRequest.Session r, boolean creating) {
        authorize(token, r.projectId(), r.chapterId());
        if (!creating && r.sessionId() == null) throw new BadRequestException("session_id is required");
        if (r.onlyGroupId() != null && !repository.groupExists(r.projectId(), r.onlyGroupId()))
            throw new BadRequestException("Group does not belong to this project");
        if (repository.audienceExists(r.chapterId(), r.onlyGroupId(), creating ? null : r.sessionId()))
            throw new ConflictException("A schedule already exists for this audience; edit that schedule instead");
        validateUrl(r.locationUrl(), "location_url", false);
        try {
            Integer id = repository.saveSession(r, creating);
            if (!r.isLocked() && r.status() == com.ailene.lms.common.Status.active) notifications.chapterUnlocked(id);
            return new IdResponse(id);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Schedule could not be saved; check its audience and trainer");
        }
    }

    @Transactional
    public AdminDeleteResponse deleteSession(String token, AdminCurriculumRequest.SessionRef r) {
        authorize(token, r.projectId(), r.chapterId());
        try { repository.deleteSession(r); }
        catch (DataIntegrityViolationException e) { throw inUse(); }
        return new AdminDeleteResponse(true);
    }

    @Transactional
    public SavedResponse saveContent(String token, AdminCurriculumRequest.Content r, boolean creating) {
        authorize(token, r.projectId(), r.chapterId());
        validateContent(r);
        String id = repository.contentId(r.chapterId(), r.kind());
        if (creating && id != null) throw new ConflictException("This session already has this content type; edit it instead");
        if (!creating && id == null) throw new ResourceNotFoundException("Content not found in this session");
        if (id != null && AdminCurriculumRepository.isAssessment(r.kind()) && repository.hasProgress(r.kind(), id))
            throw new ConflictException("This assessment already has attempts and can no longer be edited");
        try { repository.saveContent(r, id == null ? UUID.randomUUID().toString() : id); }
        catch (DataIntegrityViolationException e) { throw new ConflictException("Content could not be saved; refresh and try again"); }
        return new SavedResponse(true);
    }

    @Transactional
    public AdminDeleteResponse deleteContent(String token, AdminCurriculumRequest.ContentRef r) {
        authorize(token, r.projectId(), r.chapterId());
        String id = repository.contentId(r.chapterId(), r.kind());
        if (id == null) throw new ResourceNotFoundException("Content not found in this session");
        if (repository.hasProgress(r.kind(), id)) throw inUse();
        try { repository.deleteContent(r.kind(), id); }
        catch (DataIntegrityViolationException e) { throw inUse(); }
        return new AdminDeleteResponse(true);
    }

    private void authorize(String token, String projectId, Integer chapterId) {
        guard.requireAdminOnProject(token, projectId);
        repository.lockChapter(projectId, chapterId);
    }

    static void validateContent(AdminCurriculumRequest.Content r) {
        validateUrl(r.fileUrl(), "file_url", false);
        validateUrl(r.imageUrl(), "image_url", false);
        validateUrl(r.videoUrl(), "video_url", r.kind() == ContentKind.video);
        if (AdminCurriculumRepository.isAssessment(r.kind())) {
            if (r.questions().isEmpty()) throw new BadRequestException("Add at least one question");
            for (var q : r.questions()) {
                if (r.kind() == ContentKind.quiz && !"multiple_choice".equals(q.type()))
                    throw new BadRequestException("Quiz questions must be multiple choice");
                if ("essay".equals(q.type())) {
                    if (!q.options().isEmpty()) throw new BadRequestException("Essay questions cannot have answer options");
                } else {
                    if (q.options().size() < 2 || q.options().stream().filter(o -> Boolean.TRUE.equals(o.isCorrect())).count() != 1)
                        throw new BadRequestException("Each multiple-choice question needs at least two options and exactly one correct answer");
                    var codes = new HashSet<String>();
                    for (var o : q.options()) if (!codes.add(o.optionCode())) throw new BadRequestException("Answer option codes must be unique");
                }
            }
        } else if (!r.questions().isEmpty()) throw new BadRequestException("Only assessments can contain questions");
        if (r.kind() != ContentKind.video && !r.timestamps().isEmpty()) throw new BadRequestException("Only videos can contain timestamps");
        var starts = new HashSet<Integer>();
        for (var t : r.timestamps()) if (!starts.add(t.startSeconds())) throw new BadRequestException("Video timestamps must have unique start times");
    }

    private static void validateUrl(String value, String field, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw new BadRequestException(field + " is required");
            return;
        }
        try {
            URI url = URI.create(value);
            if (("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme())) && url.getHost() != null) return;
        } catch (IllegalArgumentException ignored) {}
        throw new BadRequestException(field + " must be an absolute HTTP or HTTPS URL");
    }

    private static ConflictException inUse() { return new ConflictException("This content or schedule already has learner progress or survey responses and cannot be deleted"); }
    public record IdResponse(Integer id) {}
    public record SavedResponse(boolean saved) {}
}
