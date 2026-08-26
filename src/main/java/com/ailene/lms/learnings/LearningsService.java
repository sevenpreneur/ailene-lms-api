package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterListProjection;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.LevelMaterialProjection;
import com.ailene.lms.material.Material;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningsService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;
    private final QuizRepository quizRepository;
    private final VideoRepository videoRepository;
    private final MaterialRepository materialRepository;

    public LearningsResponse getTasks(String jwt, LearningsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Chapter chapter = resolveChapter(request.chapterId());
        Access access = resolveAccess(userId, chapter).access();

        List<QuizTaskItem> quizzes = quizRepository.findQuizTasks(request.chapterId(), access.getId()).stream()
                .map(QuizTaskItem::from)
                .toList();
        List<VideoTaskItem> videos = videoRepository.findVideoTasks(request.chapterId(), access.getId()).stream()
                .map(VideoTaskItem::from)
                .toList();
        List<MaterialTaskItem> materials = materialRepository.findMaterialTasks(request.chapterId(), access.getId())
                .stream()
                .map(MaterialTaskItem::from)
                .toList();

        return new LearningsResponse(quizzes, videos, materials);
    }

    public List<ChapterListItem> getChapters(String jwt, ChapterListRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        List<ChapterListProjection> rows = chapterRepository.findChapterList(request.projectId(), access.getId());
        return rows.stream().map(ChapterListItem::from).toList();
    }

    public List<LevelDto> getLevels(String jwt, LevelListRequest request) {
        authService.resolveUserId(jwt);

        return levelRepository.findByProjectIdAndStatusOrderByLevelNumberAsc(request.projectId(), Status.active)
                .stream()
                .map(LevelDto::from)
                .toList();
    }

    public LevelMaterialsResponse getLevelMaterials(String jwt, LevelMaterialsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Material current = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        Chapter chapter = resolveChapter(current.getChapterId());
        AccessContext ctx = resolveAccess(userId, chapter);
        Access access = ctx.access();
        Level level = ctx.level();

        StudentStatusProjection status = accessRepository.findStudentStatus(userId, level.getProjectId())
                .orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();
        boolean levelUnlocked = level.getLevelNumber() <= currentLevelNumber;

        Instant now = Instant.now();
        List<LevelMaterialItem> materials = new ArrayList<>();
        int index = 0;
        for (LevelMaterialProjection row : materialRepository.findLevelMaterials(level.getId(), access.getId())) {
            index++;
            boolean sessionStarted = !row.getSessionDate().isAfter(now);
            boolean unlocked = levelUnlocked && sessionStarted;
            materials.add(new LevelMaterialItem(row.getMaterialId(), row.getMaterialTitle(), index,
                    row.getCompleted(), !unlocked, row.getMaterialId().equals(request.materialId())));
        }

        return new LevelMaterialsResponse(level.getLevelNumber(), materials);
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private AccessContext resolveAccess(UUID userId, Chapter chapter) {
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, level.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        return new AccessContext(access, level);
    }

    private record AccessContext(Access access, Level level) {
    }
}
