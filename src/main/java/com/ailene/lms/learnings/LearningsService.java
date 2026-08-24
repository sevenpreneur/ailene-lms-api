package com.ailene.lms.learnings;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import com.ailene.lms.material.MaterialRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, level.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

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
}
