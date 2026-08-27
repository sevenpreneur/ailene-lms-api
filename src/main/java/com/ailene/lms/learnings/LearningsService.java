package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterListProjection;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.AssignedByUser;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.MaterialFocusProjection;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.prompt.PromptAssignedProjection;
import com.ailene.lms.prompt.PromptCategoryProjection;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.quiz.QuizFocusProjection;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.usecase.UseCaseAssignedProjection;
import com.ailene.lms.usecase.UseCaseCategoryProjection;
import com.ailene.lms.usecase.UseCaseRepository;
import com.ailene.lms.video.VideoFocusProjection;
import com.ailene.lms.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final PromptRepository promptRepository;
    private final UseCaseRepository useCaseRepository;

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

    public TodayFocusResponse getTodayFocus(String jwt, TodayFocusRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        String projectId = request.projectId();

        Access access = accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        StudentStatusProjection status = accessRepository.findStudentStatus(userId, projectId).orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();

        List<ChapterListProjection> chapters = chapterRepository.findChapterList(projectId, access.getId());
        Map<Integer, List<MaterialFocusProjection>> materialsByChapter = materialRepository
                .findProjectMaterialsForFocus(projectId, access.getId()).stream()
                .collect(Collectors.groupingBy(MaterialFocusProjection::getChapterId, LinkedHashMap::new,
                        Collectors.toList()));
        Map<Integer, List<QuizFocusProjection>> quizzesByChapter = quizRepository
                .findProjectQuizzesForFocus(projectId, access.getId()).stream()
                .collect(Collectors.groupingBy(QuizFocusProjection::getChapterId, LinkedHashMap::new,
                        Collectors.toList()));
        Map<Integer, List<VideoFocusProjection>> videosByChapter = videoRepository
                .findProjectVideosForFocus(projectId, access.getId()).stream()
                .collect(Collectors.groupingBy(VideoFocusProjection::getChapterId, LinkedHashMap::new,
                        Collectors.toList()));

        TodayFocusItem assignmentFocus = resolveAssignmentFocus(projectId, access.getId());

        Instant now = Instant.now();
        TodayFocusItem focus = null;
        for (ChapterListProjection chapter : chapters) {
            if (chapter.getLevelNumber() > currentLevelNumber || chapter.getSessionDate().isAfter(now)) {
                continue;
            }

            MaterialFocusProjection material = firstIncomplete(materialsByChapter.get(chapter.getId()),
                    MaterialFocusProjection::getCompleted);
            if (material != null) {
                focus = new TodayFocusItem(TodayFocusKind.material, material.getMaterialId(), material.getTitle(),
                        chapter.getId(), chapter.getName(), chapter.getLevelId(), chapter.getLevelNumber(), null,
                        null, null);
                break;
            }

            QuizFocusProjection quiz = firstIncomplete(quizzesByChapter.get(chapter.getId()),
                    QuizFocusProjection::getCompleted);
            if (quiz != null) {
                focus = new TodayFocusItem(TodayFocusKind.quiz, quiz.getQuizId(), quiz.getName(), chapter.getId(),
                        chapter.getName(), chapter.getLevelId(), chapter.getLevelNumber(), null, null, null);
                break;
            }

            if (assignmentFocus != null) {
                focus = assignmentFocus;
                break;
            }

            VideoFocusProjection video = firstIncomplete(videosByChapter.get(chapter.getId()),
                    VideoFocusProjection::getCompleted);
            if (video != null) {
                focus = new TodayFocusItem(TodayFocusKind.video, String.valueOf(video.getVideoId()),
                        video.getTitle(), chapter.getId(), chapter.getName(), chapter.getLevelId(),
                        chapter.getLevelNumber(), null, null, null);
                break;
            }
        }

        if (focus == null) {
            focus = assignmentFocus;
        }

        return new TodayFocusResponse(focus);
    }

    private <T> T firstIncomplete(List<T> items, Function<T, Boolean> completed) {
        if (items == null) {
            return null;
        }
        for (T item : items) {
            if (!Boolean.TRUE.equals(completed.apply(item))) {
                return item;
            }
        }
        return null;
    }

    private TodayFocusItem resolveAssignmentFocus(String projectId, String accessId) {
        PromptAssignedProjection prompt = promptRepository.findAssignedPrompts(projectId, accessId, false, null)
                .stream()
                .filter(p -> p.getDeadlineAt() != null)
                .findFirst()
                .orElse(null);
        UseCaseAssignedProjection useCase = useCaseRepository.findAssignedUseCases(projectId, accessId, false, null)
                .stream()
                .filter(u -> u.getDeadlineAt() != null)
                .findFirst()
                .orElse(null);

        if (prompt == null && useCase == null) {
            return null;
        }

        boolean useCaseWins = useCase != null
                && (prompt == null || useCase.getDeadlineAt().isBefore(prompt.getDeadlineAt()));

        if (useCaseWins) {
            String category = useCaseRepository.findCategoriesForUseCases(List.of(useCase.getId())).stream()
                    .findFirst()
                    .map(UseCaseCategoryProjection::getName)
                    .orElse(null);
            AssignedByUser assignedBy = useCase.getAssignedById() == null ? null
                    : new AssignedByUser(useCase.getAssignedById(), useCase.getAssignedByName(),
                            useCase.getAssignedByAvatar());
            return new TodayFocusItem(TodayFocusKind.use_case_practice, String.valueOf(useCase.getId()),
                    useCase.getName(), null, null, useCase.getLevelId(), useCase.getLevelNumber(), category,
                    assignedBy, TimeUtils.toOffsetDateTime(useCase.getDeadlineAt()));
        }

        String category = promptRepository.findCategoriesForPrompts(List.of(prompt.getId())).stream()
                .findFirst()
                .map(PromptCategoryProjection::getName)
                .orElse(null);
        AssignedByUser assignedBy = prompt.getAssignedById() == null ? null
                : new AssignedByUser(prompt.getAssignedById(), prompt.getAssignedByName(),
                        prompt.getAssignedByAvatar());
        return new TodayFocusItem(TodayFocusKind.prompt_practice, String.valueOf(prompt.getId()), prompt.getName(),
                null, null, prompt.getLevelId(), prompt.getLevelNumber(), category, assignedBy,
                TimeUtils.toOffsetDateTime(prompt.getDeadlineAt()));
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
