package com.ailene.lms.chaptertest;

import com.ailene.lms.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChapterTestScorerTest {

    private static ChapterTestQuestion choice(int id, String correctCode) {
        return new ChapterTestQuestion(id, ChapterTestQuestionType.multiple_choice, "Q" + id, null, (short) id,
                List.of(new ChapterTestQuestion.Option(id * 10 + 1, "A", "a", "A".equals(correctCode)),
                        new ChapterTestQuestion.Option(id * 10 + 2, "B", "b", "B".equals(correctCode))));
    }

    private static ChapterTestQuestion essay(int id) {
        return new ChapterTestQuestion(id, ChapterTestQuestionType.essay, "Q" + id, null, (short) id, List.of());
    }

    @Test
    void scoresOnlyMultipleChoiceAndKeepsEssayText() {
        var result = ChapterTestScorer.score(List.of(choice(1, "A"), choice(2, "B"), choice(3, "A"), essay(4)),
                Map.of("1", "A", "2", "A", "3", "A", "4", "  Saya pakai AI untuk merangkum rapat.  "));

        assertThat(result.score()).isEqualTo((short) 67);
        assertThat(result.correctCount()).isEqualTo((short) 2);
        assertThat(result.choiceCount()).isEqualTo((short) 3);
        assertThat(result.essayCount()).isEqualTo((short) 1);
        assertThat(result.answers()).containsEntry("4", "Saya pakai AI untuk merangkum rapat.");
    }

    @Test
    void essayOnlyTestHasNoScore() {
        var result = ChapterTestScorer.score(List.of(essay(1)), Map.of("1", "jawaban"));

        assertThat(result.score()).isNull();
        assertThat(result.choiceCount()).isZero();
    }

    @Test
    void rejectsBlankAnswer() {
        assertThatThrownBy(() -> ChapterTestScorer.score(List.of(choice(1, "A"), essay(2)), Map.of("1", "A", "2", " ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Answer every question before submitting.");
    }

    @Test
    void rejectsOptionCodeThatIsNotOnTheQuestion() {
        assertThatThrownBy(() -> ChapterTestScorer.score(List.of(choice(1, "A")), Map.of("1", "Z")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("'Z' is not an option for question 1");
    }

    @Test
    void rejectsUnknownQuestionId() {
        assertThatThrownBy(() -> ChapterTestScorer.score(List.of(choice(1, "A")), Map.of("1", "A", "99", "A")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unknown question id: 99");
    }

    @Test
    void rejectsOverlongEssay() {
        String tooLong = "x".repeat(ChapterTestScorer.MAX_ESSAY_LENGTH + 1);
        assertThatThrownBy(() -> ChapterTestScorer.score(List.of(essay(1)), Map.of("1", tooLong)))
                .isInstanceOf(BadRequestException.class);
    }
}
