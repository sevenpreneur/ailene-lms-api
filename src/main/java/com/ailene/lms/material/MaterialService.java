package com.ailene.lms.material;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.chapter.Chapter;
import com.ailene.lms.chapter.ChapterAccessGuard;
import com.ailene.lms.chapter.ChapterRepository;
import com.ailene.lms.chapter.ChapterSchedule;
import com.ailene.lms.common.ChapterSummary;
import com.ailene.lms.common.TimeUtils;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.level.Level;
import com.ailene.lms.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final AuthService authService;
    private final AccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final LevelRepository levelRepository;
    private final MaterialRepository materialRepository;
    private final ChapterAccessGuard chapterAccessGuard;

    public MaterialDetailsResponse getMaterialDetails(String jwt, MaterialDetailsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        Chapter chapter = resolveChapter(material.getChapterId());
        Access access = resolveAccess(userId, chapter);
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

        var completion = materialRepository.findCompletion(material.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new MaterialDetailsResponse(material.getId(), material.getTitle(), material.getDescription(),
                material.getContent(), material.getFileUrl(), material.getImageUrl(), material.getXpReward(),
                material.getOrderIndex(), new ChapterSummary(chapter.getId(), chapter.getName()),
                completedAt != null, TimeUtils.toOffsetDateTime(completedAt), material.getCreatedAt(),
                material.getUpdatedAt());
    }

    @Transactional
    public MaterialCompletionResponse completeMaterial(String jwt, MaterialCompletionRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        Chapter chapter = resolveChapter(material.getChapterId());
        Access access = resolveAccess(userId, chapter);
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());

        materialRepository.insertCompletion(material.getId(), access.getId());
        int inserted = materialRepository.insertXpEarning(material.getId(), access.getId(), material.getXpReward());
        Short xpAwarded = inserted > 0 ? material.getXpReward() : 0;

        var completion = materialRepository.findCompletion(material.getId(), access.getId());
        Instant completedAt = completion == null ? null : completion.getCompletedAt();

        return new MaterialCompletionResponse(material.getId(), true, TimeUtils.toOffsetDateTime(completedAt),
                xpAwarded);
    }

    public LevelMaterialsResponse getLevelMaterials(String jwt, LevelMaterialsRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        Material current = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        Chapter chapter = resolveChapter(current.getChapterId());
        Access access = resolveAccess(userId, chapter);
        chapterAccessGuard.requireUnlocked(chapter.getId(), access.getId());
        Level level = levelRepository.findById(chapter.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));

        List<LevelMaterialItem> materials = new ArrayList<>();
        int index = 0;
        for (LevelMaterialProjection row : materialRepository.findLevelMaterials(level.getId(), access.getId())) {
            index++;
            boolean locked = ChapterSchedule.lockedReason(row.getSessionLocked()) != null;
            materials.add(new LevelMaterialItem(row.getMaterialId(), row.getMaterialTitle(), index,
                    row.getCompleted(), locked, row.getMaterialId().equals(request.materialId())));
        }

        return new LevelMaterialsResponse(level.getLevelNumber(), materials);
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
