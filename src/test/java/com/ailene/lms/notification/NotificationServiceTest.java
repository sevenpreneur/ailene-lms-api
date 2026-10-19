package com.ailene.lms.notification;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AccessRepository accessRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationWriter notificationWriter;

    @InjectMocks
    private NotificationService notificationService;

    private static Access access(AccessRole role) {
        Access access = new Access();
        access.setId("acc-1");
        access.setProjectId("proj-1");
        access.setRole(role);
        return access;
    }

    private static Notification notification(int id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setAccessId("acc-1");
        notification.setType(NotificationType.prompt_assigned);
        notification.setTitle("Tugas prompt baru dari champion");
        notification.setRefId("42");
        return notification;
    }

    @Test
    void listSyncsRemindersThenPagesTheCallersOwnRows() {
        Access access = access(AccessRole.student);
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1")).thenReturn(Optional.of(access));
        when(notificationRepository.findByAccessIdOrderByCreatedAtDescIdDesc(eq("acc-1"), any()))
                .thenReturn(new PageImpl<>(List.of(notification(1)), PageRequest.of(0, 20), 1));
        when(notificationRepository.countByAccessIdAndReadAtIsNull("acc-1")).thenReturn(1L);

        NotificationListResponse response = notificationService.list(USER_ID,
                new NotificationListRequest("proj-1", null, null, null));

        verify(notificationWriter).syncReminders(access);
        assertThat(response.list()).extracting(NotificationItem::refId).containsExactly("42");
        assertThat(response.metapaging().totalData()).isEqualTo(1);
        assertThat(response.unreadCount()).isEqualTo(1);
    }

    @Test
    void unreadOnlyUsesTheUnreadQuery() {
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1"))
                .thenReturn(Optional.of(access(AccessRole.champion)));
        when(notificationRepository.findByAccessIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(eq("acc-1"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        notificationService.list(USER_ID, new NotificationListRequest("proj-1", true, 2, 5));

        verify(notificationRepository).findByAccessIdAndReadAtIsNullOrderByCreatedAtDescIdDesc("acc-1",
                PageRequest.of(1, 5));
        verify(notificationRepository, never()).findByAccessIdOrderByCreatedAtDescIdDesc(any(), any());
    }

    @Test
    void markReadIsScopedToTheCallersAccess() {
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-1"))
                .thenReturn(Optional.of(access(AccessRole.student)));
        when(notificationRepository.markRead("acc-1", List.of(3, 4))).thenReturn(2);
        when(notificationRepository.countByAccessIdAndReadAtIsNull("acc-1")).thenReturn(5L);

        NotificationReadResponse response = notificationService.markRead(USER_ID,
                new NotificationReadRequest("proj-1", List.of(3, 4, 3)));

        assertThat(response.updated()).isEqualTo(2);
        assertThat(response.unreadCount()).isEqualTo(5);
    }

    @Test
    void noAccessIsNotFoundAndWritesNothing() {
        when(accessRepository.findByUserIdAndProjectId(USER_ID, "proj-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.unreadCount(USER_ID, new NotificationProjectRequest("proj-2")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No access found for this project");
        verifyNoInteractions(notificationWriter, notificationRepository);
    }
}
