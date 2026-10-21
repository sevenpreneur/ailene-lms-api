package com.ailene.lms.admin;

import com.ailene.lms.admin.AdminCurriculumDto.ContentKind;
import com.ailene.lms.chapter.ChapterMethod;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.*;
import com.ailene.lms.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCurriculumServiceTest {
    @Mock AdminGuard guard;
    @Mock AdminCurriculumRepository repository;
    @Mock NotificationWriter notifications;
    @InjectMocks AdminCurriculumService service;

    private AdminCurriculumRequest.Content content(ContentKind kind, List<AdminCurriculumRequest.Question> questions) {
        return new AdminCurriculumRequest.Content("project", 1, kind, "Title", "Description", Status.active,
                "<p>Lesson</p>", null, null, "https://example.com/lesson.mp4", 10, questions, List.of());
    }

    private AdminCurriculumRequest.Question choice(boolean first, boolean second) {
        return new AdminCurriculumRequest.Question("multiple_choice", "Question?", null, 10, List.of(
                new AdminCurriculumRequest.Option("A", "First", first), new AdminCurriculumRequest.Option("B", "Second", second)));
    }

    @Test void requiresAdminBeforeReadingOrChangingCurriculum() {
        doThrow(new ForbiddenException("Admin only")).when(guard).requireAdminOnProject("token", "project");
        assertThatThrownBy(() -> service.deleteChapter("token", new AdminCurriculumRequest.ChapterRef("project", 1))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(repository);
    }

    @Test void scopesEveryMutationToItsProjectBeforeWriting() {
        doThrow(new ResourceNotFoundException("Not in project")).when(repository).lockChapter("other-project", 1);
        assertThatThrownBy(() -> service.deleteContent("token", new AdminCurriculumRequest.ContentRef("other-project", 1, ContentKind.material)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).contentId(any(), any());
    }

    @Test void refusesToReplaceAnExistingContentSlot() {
        when(repository.contentId(1, ContentKind.material)).thenReturn("material-1");
        assertThatThrownBy(() -> service.saveContent("token", content(ContentKind.material, List.of()), true)).isInstanceOf(ConflictException.class);
        verify(repository, never()).saveContent(any(), any());
    }

    @Test void rejectsAmbiguousOrMissingAnswerKeys() {
        for (var q : List.of(choice(true, true), choice(false, false))) {
            assertThatThrownBy(() -> AdminCurriculumService.validateContent(content(ContentKind.quiz, List.of(q))))
                    .isInstanceOf(BadRequestException.class).hasMessageContaining("exactly one correct");
        }
        assertThatCode(() -> AdminCurriculumService.validateContent(content(ContentKind.quiz, List.of(choice(true, false))))).doesNotThrowAnyException();
    }

    @Test void quizRejectsEssayButChapterTestAcceptsIt() {
        var essay = new AdminCurriculumRequest.Question("essay", "Explain", null, 0, List.of());
        assertThatThrownBy(() -> AdminCurriculumService.validateContent(content(ContentKind.quiz, List.of(essay)))).isInstanceOf(BadRequestException.class);
        assertThatCode(() -> AdminCurriculumService.validateContent(content(ContentKind.pre_test, List.of(essay)))).doesNotThrowAnyException();
    }

    @Test void protectsAssessmentQuestionsOnceAnAttemptExists() {
        when(repository.contentId(1, ContentKind.quiz)).thenReturn("quiz-1");
        when(repository.hasProgress(ContentKind.quiz, "quiz-1")).thenReturn(true);
        assertThatThrownBy(() -> service.saveContent("token", content(ContentKind.quiz, List.of(choice(true, false))), false)).isInstanceOf(ConflictException.class);
        verify(repository, never()).saveContent(any(), any());
    }

    @Test void checksAllProgressBeforeDeletingAnyChildContent() {
        when(repository.contentId(eq(1), any())).thenAnswer(call -> switch ((ContentKind) call.getArgument(1)) {
            case pre_test -> "test-1";
            case video -> "video-1";
            default -> null;
        });
        when(repository.hasProgress(any(), any())).thenAnswer(call -> call.getArgument(0) == ContentKind.video);
        assertThatThrownBy(() -> service.deleteChapter("token", new AdminCurriculumRequest.ChapterRef("project", 1))).isInstanceOf(ConflictException.class);
        verify(repository, never()).deleteContent(any(), any());
        verify(repository, never()).deleteChapter(any(), any());
    }

    @Test void allowsMaterialEditsWhileKeepingItsExistingIdAndCompletions() {
        var request = content(ContentKind.material, List.of());
        when(repository.contentId(1, ContentKind.material)).thenReturn("material-1");
        assertThat(service.saveContent("token", request, false).saved()).isTrue();
        verify(repository).saveContent(request, "material-1");
        verify(repository, never()).deleteContent(any(), any());
    }

    @Test void reportsReferencedSchedulesAsConflict() {
        var request = new AdminCurriculumRequest.SessionRef("project", 1, 5);
        doThrow(new DataIntegrityViolationException("survey FK")).when(repository).deleteSession(request);
        assertThatThrownBy(() -> service.deleteSession("token", request)).isInstanceOf(ConflictException.class);
    }

    @Test void rejectsGroupFromAnotherProject() {
        var request = new AdminCurriculumRequest.Session("project", 1, null, 99, OffsetDateTime.now(), 90,
                "Zoom", "https://example.com", ChapterMethod.online, null, Status.active, true);
        assertThatThrownBy(() -> service.saveSession("token", request, true)).isInstanceOf(BadRequestException.class).hasMessageContaining("Group");
        verify(repository, never()).saveSession(any(), anyBoolean());
    }

    @Test void onlyNotifiesForAnOpenActiveSchedule() {
        var request = new AdminCurriculumRequest.Session("project", 1, null, null, OffsetDateTime.now(), 90,
                "Zoom", "https://example.com", ChapterMethod.online, null, Status.active, false);
        when(repository.saveSession(request, true)).thenReturn(9);
        assertThat(service.saveSession("token", request, true).id()).isEqualTo(9);
        verify(notifications).chapterUnlocked(9);
    }

    @Test void rejectsUnsafeVideoUrls() {
        var r = content(ContentKind.video, List.of());
        var invalid = new AdminCurriculumRequest.Content(r.projectId(), r.chapterId(), r.kind(), r.title(), r.description(),
                r.status(), null, null, null, "javascript:alert(1)", 0, List.of(), List.of());
        assertThatThrownBy(() -> AdminCurriculumService.validateContent(invalid)).isInstanceOf(BadRequestException.class).hasMessageContaining("video_url");
    }
}
