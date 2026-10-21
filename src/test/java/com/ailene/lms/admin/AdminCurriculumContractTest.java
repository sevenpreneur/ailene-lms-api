package com.ailene.lms.admin;

import com.ailene.lms.admin.AdminCurriculumDto.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AdminCurriculumContractTest {
    private final JsonMapper mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

    @Test void emitsBooleanAndDateFieldsWithTheExactFrontendNames() {
        var schedule = new Session(7, null, OffsetDateTime.parse("2026-10-21T09:00:00+07:00"), 90,
                "Zoom", "", "online", null, "active", true);
        var json = mapper.readTree(mapper.writeValueAsString(schedule));
        assertThat(json.get("is_locked").asBoolean()).isTrue();
        assertThat(json.get("session_date").isString()).isTrue();
        assertThat(json.get("only_group_id").isNull()).isTrue();
        var option = mapper.readTree(mapper.writeValueAsString(new Option("A", "Correct", true)));
        assertThat(option.get("is_correct").asBoolean()).isTrue();
        assertThat(option.get("option_code").asString()).isEqualTo("A");
    }

    @Test void acceptsTheFrontendQuestionAndMarkerPayload() {
        var request = mapper.readValue("""
                {"project_id":"project","chapter_id":1,"kind":"quiz","title":"Quiz","status":"active","xp_reward":0,
                 "questions":[{"type":"multiple_choice","question":"Question?","explanation":null,"xp_reward":10,
                   "options":[{"option_code":"A","text":"Correct","is_correct":true},
                              {"option_code":"B","text":"Incorrect","is_correct":false}]}],"timestamps":[]}
                """, AdminCurriculumRequest.Content.class);
        assertThat(request.chapterId()).isEqualTo(1);
        assertThat(request.kind()).isEqualTo(ContentKind.quiz);
        assertThat(request.questions().getFirst().options().getFirst().isCorrect()).isTrue();
        AdminCurriculumService.validateContent(request);
    }

    @Test void emitsTheCompleteNestedCurriculumShape() {
        var content = new Content("quiz-id", ContentKind.quiz, "Quiz", null, "active", null, null, null, null, 0,
                List.of(new Question(1, "multiple_choice", "Question?", null, 10, List.of(new Option("A", "Yes", true)))), List.of(), true);
        var curriculum = new AdminCurriculumDto(List.of(new Level(1, "Foundation")), List.of(new Chapter(1, 1, "Session", null, "active", List.of(), List.of(content))));
        var json = mapper.readTree(mapper.writeValueAsString(curriculum)).get("chapters").get(0);
        assertThat(json.get("level_id").asInt()).isEqualTo(1);
        assertThat(json.get("contents").get(0).get("has_progress").asBoolean()).isTrue();
        assertThat(json.get("contents").get(0).get("questions").get(0).get("xp_reward").asInt()).isEqualTo(10);
    }
}
