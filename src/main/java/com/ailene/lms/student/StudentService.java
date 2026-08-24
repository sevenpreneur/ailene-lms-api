package com.ailene.lms.student;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;

    public StudentStatusResponse getStatus(String jwt, StudentStatusRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        return accessRepository.findStudentStatus(userId, request.projectId())
                .map(StudentStatusResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }

    public List<ChapterListItem> getChapters(String jwt, ChapterListRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        return chapterRepository.findChapterList(request.projectId(), access.getId()).stream()
                .map(ChapterListItem::from)
                .toList();
    }

    public List<LevelDto> getLevels(String jwt, LevelListRequest request) {
        authService.resolveUserId(jwt);

        return levelRepository.findByProjectIdAndStatusOrderByLevelNumberAsc(request.projectId(), Status.active)
                .stream()
                .map(LevelDto::from)
                .toList();
    }
}
