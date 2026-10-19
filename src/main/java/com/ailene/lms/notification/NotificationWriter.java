package com.ailene.lms.notification;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationWriter {

    static final Duration REMINDER_WINDOW = Duration.ofHours(24);
    private static final int BODY_PREVIEW_LENGTH = 200;

    private final NotificationRepository notificationRepository;

    // Reminders are written when the caller asks for notifications, so no scheduler has to keep Neon awake.
    @Transactional
    public void syncReminders(Access access) {
        if (access.getRole() == AccessRole.sponsor) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = now.plus(REMINDER_WINDOW);
        notificationRepository.insertChapterReminders(access.getId(), "Sesi chapter dimulai dalam 24 jam", now,
                until);
        if (access.getRole() == AccessRole.student) {
            notificationRepository.insertPromptDeadlineReminders(access.getId(),
                    "Deadline tugas prompt tinggal 24 jam", now, until);
            notificationRepository.insertUseCaseDeadlineReminders(access.getId(),
                    "Deadline tugas use case tinggal 24 jam", now, until);
        }
    }

    @Transactional
    public void chapterUnlocked(Integer sessionId) {
        notificationRepository.insertChapterUnlocked(sessionId, "Chapter baru sudah dibuka");
    }

    public void promptAssigned(String studentAccessId, Integer promptId, String promptName,
            OffsetDateTime deadline) {
        create(studentAccessId, NotificationType.prompt_assigned, "Tugas prompt baru dari champion", promptName,
                String.valueOf(promptId), deadline);
    }

    public void useCaseAssigned(String studentAccessId, Integer useCaseId, String useCaseName,
            OffsetDateTime deadline) {
        create(studentAccessId, NotificationType.use_case_assigned, "Tugas use case baru dari champion",
                useCaseName, String.valueOf(useCaseId), deadline);
    }

    public void promptReviewed(String studentAccessId, Integer promptId, String promptName, boolean accepted) {
        create(studentAccessId, NotificationType.prompt_reviewed,
                accepted ? "Prompt kamu diterima" : "Prompt kamu perlu direvisi", promptName,
                String.valueOf(promptId), null);
    }

    public void useCaseReviewed(String studentAccessId, Integer useCaseId, String useCaseName, boolean accepted) {
        create(studentAccessId, NotificationType.use_case_reviewed,
                accepted ? "Use case kamu diterima" : "Use case kamu perlu direvisi", useCaseName,
                String.valueOf(useCaseId), null);
    }

    public void coachingNoteReceived(String studentAccessId, Integer noteId, String championName, String text) {
        create(studentAccessId, NotificationType.coaching_note, "Catatan coaching baru dari " + championName,
                preview(text), String.valueOf(noteId), null);
    }

    public void promptSubmitted(String championAccessId, Integer submissionId, String studentAccessId,
            String promptName) {
        create(championAccessId, NotificationType.prompt_submitted,
                studentName(studentAccessId) + " mengirim prompt untuk direview", promptName,
                String.valueOf(submissionId), null);
    }

    public void useCaseSubmitted(String championAccessId, Integer submissionId, String studentAccessId,
            String useCaseName) {
        create(championAccessId, NotificationType.use_case_submitted,
                studentName(studentAccessId) + " mengirim use case untuk direview", useCaseName,
                String.valueOf(submissionId), null);
    }

    private void create(String accessId, NotificationType type, String title, String body, String refId,
            OffsetDateTime eventAt) {
        Notification notification = new Notification();
        notification.setAccessId(accessId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRefId(refId);
        notification.setEventAt(eventAt);
        notificationRepository.save(notification);
    }

    private String studentName(String accessId) {
        String name = notificationRepository.findFullName(accessId);
        return name == null ? "Peserta" : name;
    }

    private static String preview(String text) {
        String trimmed = text.strip();
        return trimmed.length() <= BODY_PREVIEW_LENGTH ? trimmed
                : trimmed.substring(0, BODY_PREVIEW_LENGTH - 1) + "…";
    }
}
