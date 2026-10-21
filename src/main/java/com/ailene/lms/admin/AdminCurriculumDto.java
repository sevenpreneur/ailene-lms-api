package com.ailene.lms.admin;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminCurriculumDto(List<Level> levels, List<Chapter> chapters) {
    public record Level(Integer id, String name) {}
    public record Chapter(Integer id, Integer levelId, String name, String description, String status,
                          List<Session> sessions, List<Content> contents) {}
    public record Session(Integer id, Integer onlyGroupId, OffsetDateTime sessionDate, Integer durationMinutes,
                          String locationName, String locationUrl, String method, String trainerId,
                          String status, boolean isLocked) {}
    public record Content(String id, ContentKind kind, String title, String description, String status,
                          String content, String fileUrl, String imageUrl, String videoUrl, Integer xpReward,
                          List<Question> questions, List<Timestamp> timestamps, boolean hasProgress) {}
    public record Question(Integer id, String type, String question, String explanation, Integer xpReward,
                           List<Option> options) {}
    public record Option(String optionCode, String text, boolean isCorrect) {}
    public record Timestamp(Integer startSeconds, String title) {}
    public enum ContentKind { pre_test, post_test, quiz, material, video }
}
