package com.ailene.lms.admin;

import com.ailene.lms.chapter.ChapterSession;
import com.ailene.lms.chapter.ChapterSessionRepository;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminChapterSessionServiceTest {

    @Mock
    private AdminGuard adminGuard;
    @Mock
    private ChapterSessionRepository chapterSessionRepository;
    @Mock
    private NotificationWriter notificationWriter;

    @InjectMocks
    private AdminChapterSessionService adminChapterSessionService;

    private static ChapterSession session(int id, Integer onlyGroupId) {
        ChapterSession session = new ChapterSession();
        session.setId(id);
        session.setChapterId(5);
        session.setProjectId("proj-1");
        session.setOnlyGroupId(onlyGroupId);
        session.setIsLocked(false);
        return session;
    }

    @Test
    void lockingAGroupSessionWritesOnlyThatRow() {
        ChapterSession groupSession = session(12, 7);
        when(chapterSessionRepository.findByIdAndProjectId(12, "proj-1")).thenReturn(Optional.of(groupSession));
        when(chapterSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminChapterSessionDto dto = adminChapterSessionService.updateLock("token",
                new UpdateChapterSessionLockRequest("proj-1", 12, true));

        ArgumentCaptor<ChapterSession> saved = ArgumentCaptor.forClass(ChapterSession.class);
        verify(chapterSessionRepository).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(groupSession);
        assertThat(saved.getValue().getOnlyGroupId()).isEqualTo(7);
        assertThat(saved.getValue().getUpdatedAt()).isNotNull();
        assertThat(dto.isLocked()).isTrue();
        assertThat(dto.id()).isEqualTo(12);
        verify(adminGuard).requireAdminOnProject("token", "proj-1");
        verifyNoInteractions(notificationWriter);
    }

    @Test
    void unlockingClearsTheOverride() {
        ChapterSession locked = session(12, null);
        locked.setIsLocked(true);
        when(chapterSessionRepository.findByIdAndProjectId(12, "proj-1")).thenReturn(Optional.of(locked));
        when(chapterSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminChapterSessionDto dto = adminChapterSessionService.updateLock("token",
                new UpdateChapterSessionLockRequest("proj-1", 12, false));

        assertThat(dto.isLocked()).isFalse();
        verify(notificationWriter).chapterUnlocked(12);
    }

    @Test
    void sessionFromAnotherProjectIsNotFound() {
        when(chapterSessionRepository.findByIdAndProjectId(12, "proj-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminChapterSessionService.updateLock("token",
                new UpdateChapterSessionLockRequest("proj-2", 12, true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Chapter session not found");
        verify(chapterSessionRepository, never()).save(any());
    }

    @Test
    void nonAdminTouchesNothing() {
        doThrow(new ForbiddenException("Only administrators can access this resource"))
                .when(adminGuard).requireAdminOnProject("token", "proj-1");

        assertThatThrownBy(() -> adminChapterSessionService.updateLock("token",
                new UpdateChapterSessionLockRequest("proj-1", 12, true)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(chapterSessionRepository);
    }
}
