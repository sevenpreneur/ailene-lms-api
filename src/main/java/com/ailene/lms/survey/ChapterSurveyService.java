package com.ailene.lms.survey;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.StudentStatusProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChapterSurveyService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;
    private final ChapterSurveyRepository chapterSurveyRepository;

    public ChapterSurveyDetailsResponse getDetails(String jwt, ChapterSurveyDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Chapter chapter = resolveChapter(request.chapterId());
        Access access = resolveAccessAndRequireUnlock(userId, chapter);
        ChapterSessionSlotProjection session = resolveSession(chapter.getId(), access.getId());

        ChapterSurveyAnswer response = chapterSurveyRepository
                .findByStudentAccessIdAndChapterId(access.getId(), chapter.getId())
                .map(ChapterSurveyAnswer::from)
                .orElse(null);

        return new ChapterSurveyDetailsResponse(new ChapterSummary(chapter.getId(), chapter.getName()),
                TimeUtils.toOffsetDateTime(session.getSessionDate()), sessionStarted(session), response);
    }

    @Transactional
    public ChapterSurveyAnswer submit(String jwt, ChapterSurveySubmitRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Chapter chapter = resolveChapter(request.chapterId());
        Access access = resolveAccessAndRequireUnlock(userId, chapter);
        ChapterSessionSlotProjection session = resolveSession(chapter.getId(), access.getId());

        if (!sessionStarted(session)) {
            throw new ForbiddenException("This survey opens once the chapter's session starts.");
        }
        if (chapterSurveyRepository.findByStudentAccessIdAndChapterId(access.getId(), chapter.getId()).isPresent()) {
            throw new ConflictException("You already submitted the survey for this chapter.");
        }

        ChapterSurvey survey = new ChapterSurvey();
        survey.setStudentAccessId(access.getId());
        survey.setChapterId(chapter.getId());
        survey.setSessionId(session.getId());
        survey.setSatisfaction(request.satisfaction());
        survey.setRelevance(request.relevance());
        survey.setTrainerRating(request.trainerRating());
        survey.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        survey.setSubmittedAt(OffsetDateTime.now());
        try {
            survey = chapterSurveyRepository.saveAndFlush(survey);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("You already submitted the survey for this chapter.");
        }
        return ChapterSurveyAnswer.from(survey);
    }

    public ChapterSurveyStatus findStatus(Integer chapterId, String accessId) {
        ChapterSessionSlotProjection session = chapterSurveyRepository.findApplicableSession(chapterId, accessId)
                .orElse(null);
        OffsetDateTime submittedAt = chapterSurveyRepository.findByStudentAccessIdAndChapterId(accessId, chapterId)
                .map(ChapterSurvey::getSubmittedAt)
                .orElse(null);
        return new ChapterSurveyStatus(session != null && sessionStarted(session), submittedAt != null, submittedAt);
    }

    private boolean sessionStarted(ChapterSessionSlotProjection session) {
        return session.getSessionDate() != null && !session.getSessionDate().isAfter(Instant.now());
    }

    private ChapterSessionSlotProjection resolveSession(Integer chapterId, String accessId) {
        return chapterSurveyRepository.findApplicableSession(chapterId, accessId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private Access resolveAccessAndRequireUnlock(UUID userId, Chapter chapter) {
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        Access access = accessRepository.findByUserIdAndProjectId(userId, chapter.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        StudentStatusProjection status = accessRepository.findStudentStatus(userId, chapter.getProjectId())
                .orElse(null);
        short currentLevelNumber = status == null || status.getCurrentLevelNumber() == null ? 0
                : status.getCurrentLevelNumber();
        if (level.getLevelNumber() > currentLevelNumber) {
            throw new ForbiddenException("This level hasn't been unlocked yet.");
        }
        return access;
    }
}
