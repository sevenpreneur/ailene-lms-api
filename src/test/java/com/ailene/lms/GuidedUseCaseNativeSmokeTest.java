package com.ailene.lms;

import com.ailene.lms.auth.JwtService;
import com.ailene.lms.common.NanoId;
import com.ailene.lms.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: run against a local native container and an isolated DB_URL branch. Uses the real AI provider. */
@SpringBootTest
@EnabledIfSystemProperty(named = "guided.native.base-url", matches = "http://127\\.0\\.0\\.1:[0-9]+")
class GuidedUseCaseNativeSmokeTest {
    static { LmsApplication.loadDotenv(); }

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private JsonNode call(String path, String token, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(System.getProperty("guided.native.base-url") + "/api/v1/" + path))
                .timeout(Duration.ofSeconds(220)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        // Never include the request or JWT in a failure message.
        assertThat(response.statusCode()).as("native %s: %s", path, response.body()).isEqualTo(200);
        return mapper.readTree(response.body()).get("data");
    }

    @Test
    void realNativeImagePersistsGeneratesChecksSubmitsAndServesChampionDetails() throws Exception {
        assertThat(System.getenv("DB_URL")).as("Set an isolated Neon branch DB_URL explicitly").isNotBlank();
        String project = NanoId.generate();
        String studentAccess = NanoId.generate();
        String championAccess = NanoId.generate();
        UUID student = UUID.randomUUID();
        UUID champion = UUID.randomUUID();
        User studentUser = new User();
        studentUser.setId(student);
        studentUser.setEmail(student + "@example.invalid");
        User championUser = new User();
        championUser.setId(champion);
        championUser.setEmail(champion + "@example.invalid");
        String studentToken = jwt.issue(studentUser);
        String championToken = jwt.issue(championUser);
        transactions.executeWithoutResult(tx -> {
            jdbc.update("INSERT INTO lms_projects(id,name) VALUES (?,?)", project, "Native guided smoke");
            int group = jdbc.queryForObject("INSERT INTO lms_groups(name,project_id) VALUES (?,?) RETURNING id",
                    Integer.class, "Native guided smoke", project);
            int level = jdbc.queryForObject("SELECT id FROM lms_levels WHERE level_number=3", Integer.class);
            for (User user : List.of(studentUser, championUser)) {
                jdbc.update("INSERT INTO lms_users(id,full_name,email,job_title) VALUES (?,?,?,?)",
                        user.getId(), "Native guided smoke", user.getEmail(), "Test");
            }
            jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'student')",
                    studentAccess, project, student, group, level);
            jdbc.update("INSERT INTO lms_accesses(id,project_id,user_id,group_id,current_level_id,role) VALUES (?,?,?,?,?,'champion')",
                    championAccess, project, champion, group, level);
            jdbc.update("INSERT INTO lms_tokens(user_id,token,is_active) VALUES (?,?,true),(?,?,true)",
                    student, studentToken, champion, championToken);
        });
        try {
            JsonNode draft = call("use-cases/guided/save", studentToken, Map.of(
                    "project_id", project, "current_step", 4,
                    "daily_task", "Saya merekap laporan penjualan mingguan dari beberapa cabang.",
                    "frequency", "weekly", "example_text", "Data simulasi: Cabang A target 50 juta, realisasi 45 juta; cabang B target 40 juta, realisasi 42 juta.",
                    "sensitive_data_confirmed", true, "minutes_without_ai", 90,
                    "current_process", "Kumpulkan data Excel, cek angka, hitung selisih, lalu tulis ringkasan.",
                    "current_tools", List.of("Excel", "ChatGPT")));
            int sessionId = draft.get("id").asInt();
            JsonNode generated = call("use-cases/guided/generate", studentToken, Map.of("session_id", sessionId));
            assertThat(generated.get("steps").size()).isBetween(3, 7);
            assertThat(generated.get("regenerate_remaining").isNull()).isTrue();
            assertThat(generated.get("is_generating").asBoolean()).isFalse();
            call("use-cases/guided/save", studentToken, Map.of("session_id", sessionId, "current_step", 5));
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<JsonNode>> checks = new ArrayList<>();
                for (JsonNode step : generated.get("steps")) {
                    checks.add(executor.submit(() -> call("use-cases/guided/check", studentToken,
                            Map.of("session_id", sessionId, "step_id", step.get("id").asInt(), "is_done", true))));
                }
                for (Future<JsonNode> check : checks) {
                    assertThat(check.get().get("done_at").asString()).isNotBlank();
                }
            }
            call("use-cases/guided/save", studentToken, Map.of("session_id", sessionId, "current_step", 6));
            Map<String, Object> outcome = Map.of("session_id", sessionId, "outcome_proof", "https://example.invalid/synthetic-proof.pdf",
                    "hours_with_ai", 0.5, "description", "Data simulasi sudah dicocokkan dengan sumber.", "ai_tool", "ChatGPT",
                    "type", "data_analysis", "testimonial", "Uji data simulasi: AI membantu merangkum laporan dan semua hasilnya diperiksa ulang sebelum digunakan.",
                    "helpfulness_rating", 4, "share_consent", false);
            JsonNode submitted = call("use-cases/guided/submit", studentToken, outcome);
            assertThat(submitted.get("hours_saved").decimalValue()).isEqualByComparingTo("1.00");
            JsonNode retry = call("use-cases/guided/submit", studentToken, outcome);
            assertThat(retry.get("submission_id")).isEqualTo(submitted.get("submission_id"));
            JsonNode reviewed = call("champion/use-cases/submission-details", championToken,
                    Map.of("project_id", project, "submission_id", submitted.get("submission_id").asInt()));
            assertThat(reviewed.get("guided_session").get("id").asInt()).isEqualTo(sessionId);
            assertThat(reviewed.get("guided_session").get("steps").get(0).get("done_at").asString()).isNotBlank();
            assertThat(reviewed.get("share_consent").asBoolean()).isFalse();
        } finally {
            transactions.executeWithoutResult(tx -> {
                jdbc.update("DELETE FROM lms_tokens WHERE user_id IN (?,?)", student, champion);
                jdbc.update("DELETE FROM lms_use_case_submissions WHERE student_access_id=?", studentAccess);
                jdbc.update("DELETE FROM lms_guided_use_case_sessions WHERE project_id=?", project);
                jdbc.update("DELETE FROM lms_use_case_categories WHERE use_case_id IN (SELECT id FROM lms_use_cases WHERE only_project_id=?)", project);
                jdbc.update("DELETE FROM lms_use_cases WHERE only_project_id=?", project);
                jdbc.update("DELETE FROM lms_accesses WHERE project_id=?", project);
                jdbc.update("DELETE FROM lms_groups WHERE project_id=?", project);
                jdbc.update("DELETE FROM lms_projects WHERE id=?", project);
                jdbc.update("DELETE FROM lms_users WHERE id IN (?,?)", student, champion);
            });
        }
    }
}
