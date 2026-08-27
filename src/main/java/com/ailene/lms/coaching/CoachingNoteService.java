package com.ailene.lms.coaching;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachingNoteService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final CoachingNoteRepository coachingNoteRepository;

    public List<CoachingNoteItem> list(String jwt, CoachingNotesRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Access access = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        boolean asChampion = request.role() == CoachingNoteRole.champion;
        return coachingNoteRepository.findNotes(access.getId(), asChampion).stream()
                .map(CoachingNoteItem::from)
                .toList();
    }

    @Transactional
    public CoachingNoteItem create(String jwt, CoachingNoteCreateRequest request) {
        UUID userId = authService.resolveUserId(jwt);
        Access championAccess = accessRepository.findByUserIdAndProjectId(userId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        if (championAccess.getRole() != AccessRole.champion) {
            throw new ForbiddenException("Only champions can send coaching notes.");
        }

        Access studentAccess = accessRepository.findById(request.studentAccessId())
                .filter(a -> a.getProjectId().equals(request.projectId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student access not found in this project"));

        CoachingNote note = new CoachingNote();
        note.setStudentAccessId(studentAccess.getId());
        note.setChampionAccessId(championAccess.getId());
        note.setText(request.text());
        Integer id = coachingNoteRepository.save(note).getId();

        return CoachingNoteItem.from(coachingNoteRepository.findNoteById(id));
    }
}
