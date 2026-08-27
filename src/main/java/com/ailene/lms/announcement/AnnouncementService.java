package com.ailene.lms.announcement;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final AnnouncementRepository announcementRepository;

    public AnnouncementResponse getDetails(String jwt, AnnouncementDetailsRequest request) {
        requireAccess(jwt, request.projectId());

        Announcement announcement = announcementRepository.findByProjectId(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        return AnnouncementResponse.from(announcement);
    }

    @Transactional
    public AnnouncementResponse upsert(String jwt, AnnouncementUpsertRequest request) {
        requireAccess(jwt, request.projectId());

        Announcement announcement = announcementRepository.findByProjectId(request.projectId())
                .orElseGet(Announcement::new);
        announcement.setProjectId(request.projectId());
        announcement.setTitle(request.title());
        announcement.setCallout(request.callout());
        announcement.setStatus(request.status());
        announcement.setStartDate(request.startDate());
        announcement.setEndDate(request.endDate());
        Integer id = announcementRepository.save(announcement).getId();

        Announcement saved = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        return AnnouncementResponse.from(saved);
    }

    private void requireAccess(String jwt, String projectId) {
        UUID userId = authService.resolveUserId(jwt);
        accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
