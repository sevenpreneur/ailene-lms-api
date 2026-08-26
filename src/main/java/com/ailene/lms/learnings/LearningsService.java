package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.Material;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.quiz.Quiz;
import com.ailene.lms.quiz.QuizQuestionOptionProjection;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.quiz.QuizStatsProjection;
import com.ailene.lms.video.Video;
import com.ailene.lms.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Access access = resolveAccess(userId, chapter);

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

    public MaterialDetailsResponse getMaterialDetails(String jwt, MaterialDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        Chapter chapter = resolveChapter(material.getChapterId());
        Access access = resolveAccess(userId, chapter);

        var completion = materialRepository.findCompletion(material.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new MaterialDetailsResponse(material.getId(), material.getTitle(), material.getDescription(),
                material.getContent(), material.getFileUrl(), material.getImageUrl(), material.getXpReward(),
                material.getOrderIndex(), new ChapterSummary(chapter.getId(), chapter.getName()),
                completedAt != null, TimeUtils.toOffsetDateTime(completedAt), material.getCreatedAt(),
                material.getUpdatedAt());
    }

    public VideoDetailsResponse getVideoDetails(String jwt, VideoDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Video video = videoRepository.findById(request.videoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        Chapter chapter = resolveChapter(video.getChapterId());
        Access access = resolveAccess(userId, chapter);

        var completion = videoRepository.findCompletion(video.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new VideoDetailsResponse(video.getId(), video.getTitle(), video.getDescription(), video.getVideoUrl(),
                video.getXpReward(), video.getOrderIndex(), new ChapterSummary(chapter.getId(), chapter.getName()),
                completedAt != null, TimeUtils.toOffsetDateTime(completedAt), video.getCreatedAt(),
                video.getUpdatedAt());
    }

    public QuizDetailsResponse getQuizDetails(String jwt, QuizDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        Chapter chapter = resolveChapter(quiz.getChapterId());
        Access access = resolveAccess(userId, chapter);

        QuizStatsProjection stats = quizRepository.findQuizStats(quiz.getId(), access.getId());

        Map<Integer, QuizQuestionOptionProjection> firstRowByQuestion = new LinkedHashMap<>();
        Map<Integer, List<QuizOptionItem>> optionsByQuestion = new LinkedHashMap<>();
        for (QuizQuestionOptionProjection row : quizRepository.findQuestionsWithOptions(quiz.getId())) {
            firstRowByQuestion.putIfAbsent(row.getQuestionId(), row);
            optionsByQuestion.computeIfAbsent(row.getQuestionId(), id -> new ArrayList<>())
                    .add(new QuizOptionItem(row.getOptionId(), row.getOptionCode(), row.getOptionText()));
        }
        List<QuizQuestionItem> questions = firstRowByQuestion.values().stream()
                .map(row -> new QuizQuestionItem(row.getQuestionId(), row.getQuestion(), row.getQuestionOrderIndex(),
                        row.getQuestionXpReward(), optionsByQuestion.get(row.getQuestionId())))
                .toList();

        return new QuizDetailsResponse(quiz.getId(), quiz.getName(), quiz.getDescription(), quiz.getOrderIndex(),
                new ChapterSummary(chapter.getId(), chapter.getName()), stats.getQuestionCount(),
                stats.getXpReward(), stats.getAttempts(), questions);
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private Access resolveAccess(UUID userId, Chapter chapter) {
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        return accessRepository.findByUserIdAndProjectId(userId, level.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
