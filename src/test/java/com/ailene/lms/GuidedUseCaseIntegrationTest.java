package com.ailene.lms;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.NanoId;
import com.ailene.lms.common.deepseek.DeepSeekClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class GuidedUseCaseIntegrationTest {
    static {
        LmsApplication.loadDotenv();
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean AuthService auth;
    @MockitoBean DeepSeekClient deepSeek;
    private MockMvc mvc;
    private String project;
    private String studentAccess;
    private String championAccess;
    private UUID student;
    private UUID other;
    private UUID champion;
    private UUID sponsor;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        project = NanoId.generate();
        studentAccess = NanoId.generate();
        championAccess = NanoId.generate();
        student = UUID.randomUUID();
        other = UUID.randomUUID();
        champion = UUID.randomUUID();
        sponsor = UUID.randomUUID();
        jdbc.update("INSERT INTO lms_projects(id,name) VALUES (?,?)", project, "Guided test project");
        int group = jdbc.queryForObject("INSERT INTO lms_groups(name,project_id) VALUES (?,?) RETURNING id",
                Integer.class, "Guided test group", project);
        int level = jdbc.queryForObject("SELECT id FROM lms_levels WHERE level_number=3", Integer.class);
        for (UUID user : List.of(student, other, champion, sponsor)) {
            jdbc.update("INSERT INTO lms_users(id,full_name,email,job_title) VALUES (?,?,?,?)",
                    user, "Guided test", user + "@example.invalid", "Test");
        }
        jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'student')",
                studentAccess, project, student, group, level);
        jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'student')",
                NanoId.generate(), project, other, group, level);
        jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'champion')",
                championAccess, project, champion, group, level);
        jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'sponsor')",
                NanoId.generate(), project, sponsor, group, level);
        when(auth.resolveUserId("student")).thenReturn(student);
        when(auth.resolveUserId("other")).thenReturn(other);
        when(auth.resolveUserId("champion")).thenReturn(champion);
        when(auth.resolveUserId("sponsor")).thenReturn(sponsor);
        when(deepSeek.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble())).thenReturn(workflow());
    }

    private String workflow() {
        return """
                {"summary":"Rekap laporan","estimated_minutes_with_ai":35,"risks":["Cek ulang angka"],"steps":[
                {"order":1,"title":"Samarkan","instruction":"Samarkan data.","suggested_tool":"Manual","prompt_template":"","estimated_minutes":10,"verification":"Tidak ada identitas."},
                {"order":2,"title":"Ringkas","instruction":"Buat ringkasan.","suggested_tool":"ChatGPT","prompt_template":"Ringkas [DATA] menjadi tabel tanpa mengarang angka.","estimated_minutes":15,"verification":"Cocokkan angka."},
                {"order":3,"title":"Periksa","instruction":"Cek hasil.","suggested_tool":"Excel","prompt_template":"","estimated_minutes":10,"verification":"Cek total."}]}
                """;
    }

    private JsonNode call(String token, String endpoint, Object body) throws Exception {
        String response = mvc.perform(post("/api/v1/use-cases/guided/" + endpoint)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("data");
    }

    private JsonNode ready() throws Exception {
        return ready("student");
    }

    private JsonNode ready(String token) throws Exception {
        return call(token, "save", Map.of("project_id", project, "current_step", 4,
                "daily_task", "Saya merekap laporan penjualan mingguan dari beberapa cabang.",
                "frequency", "weekly", "example_text", "Cabang A: target 50 juta, realisasi 45 juta.",
                "sensitive_data_confirmed", true, "minutes_without_ai", 90,
                "current_process", "Kumpulkan data, cek angka, lalu tulis ringkasan.",
                "current_tools", List.of("Excel")));
    }

    private JsonNode generated() throws Exception {
        return call("student", "generate", Map.of("session_id", ready().get("id").asInt()));
    }

    private JsonNode complete() throws Exception {
        return complete("student");
    }

    private JsonNode complete(String token) throws Exception {
        JsonNode draft = ready(token);
        int id = draft.get("id").asInt();
        JsonNode generated = call(token, "generate", Map.of("session_id", id));
        call(token, "save", Map.of("session_id", id, "current_step", 5));
        for (JsonNode step : generated.get("steps")) {
            call(token, "check", Map.of("session_id", id, "step_id", step.get("id").asInt(), "is_done", true));
        }
        return call(token, "save", Map.of("session_id", id, "current_step", 6));
    }

    private Map<String, Object> outcome(int id) {
        return Map.of("session_id", id, "outcome_proof", "https://example.test/proof.pdf", "hours_with_ai", 0.5,
                "description", "Ringkasan sudah dicocokkan dengan sumber.", "ai_tool", "ChatGPT", "type", "data_analysis",
                "testimonial", "AI membantu menyusun laporan lebih rapi. Saya tetap memeriksa semua angka sebelum membagikannya.",
                "helpfulness_rating", 4, "share_consent", false);
    }

    @Test
    void draftIsUpsertedAndCanBeResumedWithoutCallingAi() throws Exception {
        int id = ready().get("id").asInt();
        JsonNode updated = call("student", "save", Map.of("project_id", project, "current_step", 2,
                "daily_task", "Saya merekap data penjualan harian untuk laporan mingguan."));
        assertThat(updated.get("id").asInt()).isEqualTo(id);
        JsonNode resumed = call("student", "details", Map.of("project_id", project));
        assertThat(resumed.get("current_step").asInt()).isEqualTo(2);
        assertThat(resumed.get("created_at").asString()).isNotBlank();
        assertThat(resumed.get("regenerate_remaining").isNull()).isTrue();
        verifyNoInteractions(deepSeek);
    }

    @Test
    void anotherUserCannotReadOrChangeStudentSessionAndSponsorCannotCreate() throws Exception {
        int id = ready().get("id").asInt();
        for (String endpoint : List.of("details", "save", "generate")) {
            Map<String, Object> body = endpoint.equals("save")
                    ? Map.of("session_id", id, "current_step", 1) : Map.of("session_id", id);
            mvc.perform(post("/api/v1/use-cases/guided/" + endpoint).header("Authorization", "Bearer other")
                    .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(post("/api/v1/use-cases/guided/save").header("Authorization", "Bearer sponsor")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                        Map.of("project_id", project, "current_step", 1)))).andExpect(status().isForbidden());
    }

    @Test
    void championCanCompleteOwnGuidedUseCaseInStudentModeWithoutSharingDraft() throws Exception {
        int id = complete("champion").get("id").asInt();
        assertThat(call("champion", "details", Map.of("project_id", project)).get("id").asInt()).isEqualTo(id);
        assertThat(call("champion", "details", Map.of("session_id", id)).get("current_step").asInt()).isEqualTo(6);
        mvc.perform(post("/api/v1/use-cases/guided/details").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/use-cases/guided/details").header("Authorization", "Bearer sponsor")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id))))
                .andExpect(status().isNotFound());
        JsonNode submitted = call("champion", "submit", outcome(id));
        assertThat(submitted.get("submission_status").asString()).isEqualTo("awaiting_review");
        assertThat(jdbc.queryForObject("SELECT student_access_id FROM lms_use_case_submissions WHERE guided_session_id=?",
                String.class, id)).isEqualTo(championAccess);
        assertThat(call("champion", "submit", outcome(id)).get("submission_id").asInt())
                .isEqualTo(submitted.get("submission_id").asInt());
    }

    @Test
    void generationHasNoQuotaAuditsVersionsAndRejectsStaleChecklistIds() throws Exception {
        JsonNode first = generated();
        int id = first.get("id").asInt();
        int oldStep = first.get("steps").get(0).get("id").asInt();
        assertThat(first.get("regenerate_remaining").isNull()).isTrue();
        JsonNode second = call("student", "generate", Map.of("session_id", id, "feedback", "Gunakan Copilot."));
        assertThat(second.get("steps").get(0).get("id").asInt()).isNotEqualTo(oldStep);
        call("student", "save", Map.of("session_id", id, "current_step", 5));
        mvc.perform(post("/api/v1/use-cases/guided/check").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                        Map.of("session_id", id, "step_id", oldStep, "is_done", true)))).andExpect(status().isNotFound());
        call("student", "generate", Map.of("session_id", id));
        call("student", "generate", Map.of("session_id", id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lms_guided_use_case_generations WHERE session_id=?", Integer.class, id))
                .isEqualTo(4);
        mvc.perform(post("/api/v1/use-cases/guided/save").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "session_id", id, "current_step", 2, "daily_task", "Saya menyiapkan pekerjaan lain dengan konteks berbeda."))))
                .andExpect(status().isOk());
        entityManager.clear();
        assertThat(call("student", "details", Map.of("session_id", id)).get("steps").size()).isZero();
    }

    @Test
    void invalidAiOutputReleasesReservationWithoutLosingPreviousWorkflow() throws Exception {
        JsonNode generated = generated();
        int id = generated.get("id").asInt();
        when(deepSeek.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble())).thenReturn("{}");
        mvc.perform(post("/api/v1/use-cases/guided/generate").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id))))
                .andExpect(status().isBadGateway());
        JsonNode session = call("student", "details", Map.of("session_id", id));
        assertThat(session.get("regenerate_remaining").isNull()).isTrue();
        assertThat(session.get("is_generating").asBoolean()).isFalse();
        assertThat(session.get("steps")).isEqualTo(generated.get("steps"));
    }

    @Test
    void editingIntakeInvalidatesWorkflowAndChangingExampleRequiresConfirmationAgain() throws Exception {
        JsonNode s = generated();
        JsonNode changed = call("student", "save", Map.of("session_id", s.get("id").asInt(), "current_step", 2,
                "example_text", "Cabang B: target 40 juta dan realisasi 42 juta."));
        assertThat(changed.get("sensitive_data_confirmed").asBoolean()).isFalse();
        assertThat(changed.get("steps").size()).isZero();
        assertThat(changed.get("status").asString()).isEqualTo("intake");
        assertThat(changed.get("regenerate_remaining").isNull()).isTrue();
    }

    @Test
    void rejectsSensitiveDataAndMissingSkipReasonAndGeneratingMutation() throws Exception {
        int id = ready().get("id").asInt();
        mvc.perform(post("/api/v1/use-cases/guided/save").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "session_id", id, "current_step", 2, "example_text", "NIK 1234567890123456"))))
                .andExpect(status().isBadRequest());
        entityManager.clear();
        JsonNode generated = call("student", "generate", Map.of("session_id", id));
        call("student", "save", Map.of("session_id", id, "current_step", 5));
        mvc.perform(post("/api/v1/use-cases/guided/save").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id, "current_step", 6))))
                .andExpect(status().isBadRequest());
        call("student", "check", Map.of("session_id", id, "step_id", generated.get("steps").get(0).get("id").asInt(),
                "is_done", false, "skipped_reason", "Sudah disiapkan sebelumnya."));
        jdbc.update("UPDATE lms_guided_use_case_sessions SET generation_token=?, generation_started_at=CURRENT_TIMESTAMP WHERE id=?",
                NanoId.generate(), id);
        entityManager.clear();
        mvc.perform(post("/api/v1/use-cases/guided/generate").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id))))
                .andExpect(status().isConflict());
    }

    @Test
    void createsSubmissionOnceComputesSavingsAndExposesGuidedDetailsToChampion() throws Exception {
        int id = complete().get("id").asInt();
        JsonNode submitted = call("student", "submit", outcome(id));
        assertThat(submitted.get("hours_saved").decimalValue()).isEqualByComparingTo("1.00");
        assertThat(submitted.get("submission_status").asString()).isEqualTo("awaiting_review");
        assertThat(submitted.get("session").get("status").asString()).isEqualTo("submitted");
        int submissionId = submitted.get("submission_id").asInt();
        JsonNode duplicate = call("student", "submit", outcome(id));
        assertThat(duplicate.get("submission_id").asInt()).isEqualTo(submissionId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lms_use_case_submissions WHERE guided_session_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT hours_without_ai FROM lms_use_case_submissions WHERE id=?", java.math.BigDecimal.class, submissionId))
                .isEqualByComparingTo("1.50");
        mvc.perform(post("/api/v1/champion/use-cases/submission-details").header("Authorization", "Bearer champion")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                        Map.of("project_id", project, "submission_id", submissionId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.guided_session.id").value(id))
                .andExpect(jsonPath("$.data.helpfulness_rating").value(4))
                .andExpect(jsonPath("$.data.share_consent").value(false));
    }

    @Test
    void revisionResubmitsSameSubmissionThroughExistingEndpointAndKeepsGuidedBaseline() throws Exception {
        int id = complete().get("id").asInt();
        JsonNode submitted = call("student", "submit", outcome(id));
        int submissionId = submitted.get("submission_id").asInt();
        mvc.perform(post("/api/v1/champion/use-cases/review").header("Authorization", "Bearer champion")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "project_id", project, "submission_id", submissionId, "is_accepted", false,
                        "comment", "Tambahkan bukti hasil yang sudah diperbaiki."))))
                .andExpect(status().isOk());
        Map<String, Object> revision = new java.util.HashMap<>(outcome(id));
        revision.remove("session_id");
        revision.remove("testimonial");
        revision.remove("helpfulness_rating");
        revision.remove("share_consent");
        revision.put("use_case_id", submitted.get("use_case_id").asInt());
        revision.put("hours_with_ai", 0.25);
        revision.put("hours_without_ai", 9999);
        revision.put("frequency", "daily");
        mvc.perform(post("/api/v1/use-cases/submit").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(revision)))
                .andExpect(status().isOk());
        JsonNode latest = call("student", "submit", outcome(id));
        assertThat(latest.get("submission_id").asInt()).isEqualTo(submissionId);
        assertThat(latest.get("submission_status").asString()).isEqualTo("awaiting_review");
        assertThat(latest.get("hours_saved").decimalValue()).isEqualByComparingTo("1.25");
        assertThat(jdbc.queryForObject("SELECT frequency::text FROM lms_use_case_submissions WHERE id=?", String.class, submissionId))
                .isEqualTo("weekly");
        assertThat(jdbc.queryForObject("SELECT helpfulness_rating FROM lms_use_case_submissions WHERE id=?", Integer.class, submissionId))
                .isEqualTo(4);
    }

    @Test
    void librarySessionBindsExistingSubmitToCorrectCaseAndUsesSavedBaseline() throws Exception {
        int useCaseId = jdbc.queryForObject("""
                INSERT INTO lms_use_cases(name,description,level_id,only_project_id)
                SELECT 'Guided library test','Synthetic library work',id,? FROM lms_levels WHERE level_number=3
                RETURNING id
                """, Integer.class, project);
        JsonNode draft = call("student", "save", Map.of("use_case_id", useCaseId, "current_step", 4,
                "daily_task", "Saya merekap laporan penjualan mingguan dari beberapa cabang.",
                "frequency", "weekly", "example_text", "Cabang A: target 50 juta, realisasi 45 juta.",
                "sensitive_data_confirmed", true, "minutes_without_ai", 90,
                "current_process", "Kumpulkan data, cek angka, lalu tulis ringkasan."));
        int id = draft.get("id").asInt();
        JsonNode generated = call("student", "generate", Map.of("session_id", id));
        call("student", "save", Map.of("session_id", id, "current_step", 5));
        for (JsonNode step : generated.get("steps")) {
            call("student", "check", Map.of("session_id", id, "step_id", step.get("id").asInt(), "is_done", true));
        }
        Map<String, Object> legacy = new java.util.HashMap<>(outcome(id));
        legacy.remove("session_id");
        legacy.put("guided_session_id", id);
        legacy.put("use_case_id", useCaseId + 1000000);
        legacy.put("hours_without_ai", 9999);
        legacy.put("frequency", "daily");
        mvc.perform(post("/api/v1/use-cases/submit").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(legacy)))
                .andExpect(status().isBadRequest());
        legacy.put("use_case_id", useCaseId);
        mvc.perform(post("/api/v1/use-cases/submit").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(legacy)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT hours_without_ai FROM lms_use_case_submissions WHERE guided_session_id=?",
                java.math.BigDecimal.class, id)).isEqualByComparingTo("1.50");
        assertThat(jdbc.queryForObject("SELECT frequency::text FROM lms_use_case_submissions WHERE guided_session_id=?",
                String.class, id)).isEqualTo("weekly");
        assertThat(call("student", "details", Map.of("session_id", id)).get("use_case_id").asInt()).isEqualTo(useCaseId);
    }

    @Test
    void expiredGenerationCanBeRetriedAndProviderFailureRefundsAttempt() throws Exception {
        int id = ready().get("id").asInt();
        jdbc.update("UPDATE lms_guided_use_case_sessions SET generation_token=?, generation_started_at=CURRENT_TIMESTAMP - INTERVAL '6 minutes', generations_used=1 WHERE id=?",
                NanoId.generate(), id);
        entityManager.clear();
        assertThat(call("student", "details", Map.of("session_id", id)).get("is_generating").asBoolean()).isFalse();
        when(deepSeek.createJsonCompletion(anyString(), anyString(), isNull(), anyDouble()))
                .thenThrow(new com.ailene.lms.common.exception.ServiceUnavailableException("AI unavailable"));
        mvc.perform(post("/api/v1/use-cases/guided/generate").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("session_id", id))))
                .andExpect(status().isServiceUnavailable());
        JsonNode latest = call("student", "details", Map.of("session_id", id));
        assertThat(latest.get("regenerate_remaining").isNull()).isTrue();
        assertThat(latest.get("is_generating").asBoolean()).isFalse();
    }

    @Test
    void missingBearerAndInvalidRatingAreRejected() throws Exception {
        mvc.perform(post("/api/v1/use-cases/guided/details").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("project_id", project)))).andExpect(status().isUnauthorized());
        int id = complete().get("id").asInt();
        Map<String, Object> invalid = new java.util.HashMap<>(outcome(id));
        invalid.put("helpfulness_rating", 8);
        mvc.perform(post("/api/v1/use-cases/guided/submit").header("Authorization", "Bearer student")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
