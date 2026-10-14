package com.ailene.lms.survey;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.chapter.ChapterSchedule;
import com.ailene.lms.chapter.ChapterSession;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
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
    private final ChapterSurveyRepository chapterSurveyRepository;
    private final ChapterAccessGuard chapterAccessGuard;

    public ChapterSurveyDetailsResponse getDetails(String jwt, ChapterSurveyDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Chapter chapter = resolveChapter(request.chapterId());
        Access access = resolveAccess(userId, chapter);
        ChapterSession session = chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

        ChapterSurveyAnswer response = chapterSurveyRepository
                .findByStudentAccessIdAndChapterId(access.getId(), chapter.getId())
                .map(ChapterSurveyAnswer::from)
                .orElse(null);

        return new ChapterSurveyDetailsResponse(new ChapterSummary(chapter.getId(), chapter.getName()),
                session.getSessionDate(), true, response);
    }

    @Transactional
    public ChapterSurveyAnswer submit(String jwt, ChapterSurveySubmitRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Chapter chapter = resolveChapter(request.chapterId());
        Access access = resolveAccess(userId, chapter);
        ChapterSession session = chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

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
        ChapterSession session = chapterAccessGuard.findSession(chapterId, accessId).orElse(null);
        OffsetDateTime submittedAt = chapterSurveyRepository.findByStudentAccessIdAndChapterId(accessId, chapterId)
                .map(ChapterSurvey::getSubmittedAt)
                .orElse(null);
        boolean available = session != null && ChapterSchedule.lockedReason(session, Instant.now()) == null;
        return new ChapterSurveyStatus(available, submittedAt != null, submittedAt);
    }

    private Chapter resolveChapter(Integer chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private Access resolveAccess(UUID userId, Chapter chapter) {
        return accessRepository.findByUserIdAndProjectId(userId, chapter.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
