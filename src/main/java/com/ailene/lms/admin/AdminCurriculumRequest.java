package com.ailene.lms.admin;

import com.ailene.lms.common.Status;
import com.ailene.lms.chapter.ChapterMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import com.ailene.lms.admin.AdminCurriculumDto.ContentKind;

public final class AdminCurriculumRequest {
    private AdminCurriculumRequest() {}

    public record Chapter(@NotBlank String projectId, @Positive Integer chapterId,
                          @NotNull @Positive Integer levelId, @NotBlank @Size(max = 255) String name,
                          @Size(max = 10000) String description, @NotNull Status status) {}
    public record ChapterRef(@NotBlank String projectId, @NotNull @Positive Integer chapterId) {}
    public record Session(@NotBlank String projectId, @NotNull @Positive Integer chapterId,
                          @Positive Integer sessionId, @Positive Integer onlyGroupId,
                          @NotNull OffsetDateTime sessionDate, @NotNull @Min(1) @Max(1440) Integer durationMinutes,
                          @NotBlank @Size(max = 255) String locationName, @Size(max = 2000) String locationUrl,
                          @NotNull ChapterMethod method, @Size(min = 21, max = 21) String trainerId,
                          @NotNull Status status, @NotNull Boolean isLocked) {}
    public record SessionRef(@NotBlank String projectId, @NotNull @Positive Integer chapterId,
                             @NotNull @Positive Integer sessionId) {}
    public record ContentRef(@NotBlank String projectId, @NotNull @Positive Integer chapterId,
                             @NotNull ContentKind kind) {}
    public record Content(@NotBlank String projectId, @NotNull @Positive Integer chapterId,
                          @NotNull ContentKind kind, @NotBlank @Size(max = 255) String title,
                          @Size(max = 10000) String description, @NotNull Status status,
                          @Size(max = 500000) String content, @Size(max = 2000) String fileUrl,
                          @Size(max = 2000) String imageUrl, @Size(max = 2000) String videoUrl,
                          @NotNull @Min(0) @Max(32767) Integer xpReward,
                          @NotNull @Size(max = 200) List<@NotNull @Valid Question> questions,
                          @NotNull @Size(max = 200) List<@NotNull @Valid Timestamp> timestamps) {}
    public record Question(@NotBlank @Pattern(regexp = "multiple_choice|essay") String type,
                           @NotBlank @Size(max = 20000) String question, @Size(max = 20000) String explanation,
                           @NotNull @Min(0) @Max(32767) Integer xpReward,
                           @NotNull @Size(max = 26) List<@NotNull @Valid Option> options) {}
    public record Option(@NotBlank @Pattern(regexp = "[A-Z]") String optionCode,
                         @NotBlank @Size(max = 10000) String text, @NotNull Boolean isCorrect) {}
    public record Timestamp(@NotNull @Min(0) Integer startSeconds, @NotBlank @Size(max = 255) String title) {}
}
