package com.ailene.lms.chaptertest;

import com.ailene.lms.common.exception.BadRequestException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChapterTestScorer {

    static final int MAX_ESSAY_LENGTH = 5000;

    private ChapterTestScorer() {
    }

    public record Result(Short score, short correctCount, short choiceCount, short essayCount,
            Map<String, String> answers) {
    }

    // Only multiple-choice questions are scored; essays are stored for a human to read.
    public static Result score(List<ChapterTestQuestion> questions, Map<String, String> answers) {
        Set<String> knownIds = new HashSet<>();
        questions.forEach(q -> knownIds.add(String.valueOf(q.id())));
        for (String key : answers.keySet()) {
            if (!knownIds.contains(key)) {
                throw new BadRequestException("Unknown question id: " + key);
            }
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        int correct = 0;
        int choices = 0;
        int essays = 0;
        for (ChapterTestQuestion question : questions) {
            String key = String.valueOf(question.id());
            String answer = answers.get(key) == null ? "" : answers.get(key).trim();
            if (answer.isEmpty()) {
                throw new BadRequestException("Answer every question before submitting.");
            }
            if (question.isMultipleChoice()) {
                choices++;
                ChapterTestQuestion.Option picked = question.options().stream()
                        .filter(o -> o.optionCode().equals(answer))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException(
                                "'" + answer + "' is not an option for question " + key));
                if (picked.correct()) {
                    correct++;
                }
            } else {
                essays++;
                if (answer.length() > MAX_ESSAY_LENGTH) {
                    throw new BadRequestException(
                            "The answer to question " + key + " is longer than " + MAX_ESSAY_LENGTH + " characters.");
                }
            }
            normalized.put(key, answer);
        }

        Short score = choices == 0 ? null : (short) Math.round((correct * 100.0) / choices);
        return new Result(score, (short) correct, (short) choices, (short) essays, normalized);
    }
}
