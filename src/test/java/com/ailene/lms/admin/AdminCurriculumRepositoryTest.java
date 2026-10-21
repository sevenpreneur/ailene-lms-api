package com.ailene.lms.admin;

import com.ailene.lms.admin.AdminCurriculumDto.ContentKind;
import com.ailene.lms.chapter.ChapterMethod;
import com.ailene.lms.common.Status;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

// Explicitly opt in to the disposable local container; this test never reads .env or boots the application.
@EnabledIfEnvironmentVariable(named = "LMS_CURRICULUM_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55437/lms_curriculum_test")
class AdminCurriculumRepositoryTest {
    Connection connection;
    AdminCurriculumRepository repository;
    JdbcTemplate jdbc;
    Integer chapterId;
    static final String PROJECT = "curriculum-project-01";
    static final Set<String> TABLES = Set.of("lms_levels", "lms_projects", "lms_groups", "lms_chapters", "lms_chapter_sessions",
            "lms_materials", "lms_videos", "lms_video_timestamps", "lms_quizzes", "lms_quiz_questions", "lms_quiz_options",
            "lms_chapter_tests", "lms_chapter_test_questions", "lms_chapter_test_options", "lms_material_completions",
            "lms_video_completions", "lms_quiz_submissions", "lms_chapter_test_submissions", "lms_chapter_surveys");

    @BeforeEach void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("LMS_CURRICULUM_TEST_DB_URL"), "postgres", "local-curriculum-test");
        connection.setAutoCommit(false);
        var source = new SingleConnectionDataSource(connection, true);
        jdbc = new JdbcTemplate(source);
        repository = new AdminCurriculumRepository(source);
        String schema = Files.readString(Path.of("docs/db/ailene-lms.sql"));
        var types = Pattern.compile("CREATE TYPE \\w+ AS ENUM \\(.*?\\);", Pattern.DOTALL).matcher(schema);
        while (types.find()) jdbc.execute(types.group());
        var tables = Pattern.compile("CREATE TABLE (\\w+) \\(.*?\\n\\);", Pattern.DOTALL).matcher(schema);
        while (tables.find()) if (TABLES.contains(tables.group(1))) jdbc.execute(tables.group());
        var alters = Pattern.compile("ALTER TABLE (\\w+)\\s+(ADD FOREIGN KEY.*?);", Pattern.DOTALL).matcher(schema);
        while (alters.find()) {
            if (!TABLES.contains(alters.group(1))) continue;
            var keys = Pattern.compile("ADD FOREIGN KEY[^\\n]*REFERENCES (\\w+) \\([^\\n;]*?\\)(?: ON DELETE \\w+(?: \\w+)?)?").matcher(alters.group(2));
            while (keys.find()) if (TABLES.contains(keys.group(1))) jdbc.execute("ALTER TABLE " + alters.group(1) + " " + keys.group());
        }
        jdbc.update("INSERT INTO lms_projects(id, name) VALUES (?, 'Curriculum test')", PROJECT);
        jdbc.update("INSERT INTO lms_levels(id, level_number, name) VALUES (1, 1, 'Foundation')");
        chapterId = repository.createChapter(new AdminCurriculumRequest.Chapter(PROJECT, null, 1, "Session", "Description", Status.active));
    }

    @AfterEach void tearDown() throws Exception {
        if (connection != null) { connection.rollback(); connection.close(); }
    }

    AdminCurriculumRequest.Content content(ContentKind kind, String title) {
        var question = new AdminCurriculumRequest.Question("multiple_choice", "Question?", "Explanation", 5, List.of(
                new AdminCurriculumRequest.Option("A", "Correct", true), new AdminCurriculumRequest.Option("B", "Wrong", false)));
        return new AdminCurriculumRequest.Content(PROJECT, chapterId, kind, title, "Description", Status.active,
                "<p>Material</p>", "https://example.com/file.pdf", "https://example.com/cover.webp", "https://example.com/video", 10,
                AdminCurriculumRepository.isAssessment(kind) ? List.of(question) : List.of(),
                kind == ContentKind.video ? List.of(new AdminCurriculumRequest.Timestamp(0, "Introduction")) : List.of());
    }

    @Test void createsAndReadsEveryContentTypeWithAnswerKeysAndMarkers() {
        for (ContentKind kind : ContentKind.values()) repository.saveContent(content(kind, kind.name()), "content-" + kind.name());
        var result = repository.list(PROJECT);
        assertThat(result.levels()).hasSize(1);
        assertThat(result.chapters()).hasSize(1);
        var contents = result.chapters().getFirst().contents();
        assertThat(contents).hasSize(5).allSatisfy(c -> assertThat(c.hasProgress()).isFalse());
        var quiz = contents.stream().filter(c -> c.kind() == ContentKind.quiz).findFirst().orElseThrow();
        assertThat(quiz.questions()).hasSize(1);
        assertThat(quiz.questions().getFirst().options()).extracting(AdminCurriculumDto.Option::isCorrect).containsExactly(true, false);
        var video = contents.stream().filter(c -> c.kind() == ContentKind.video).findFirst().orElseThrow();
        assertThat(video.timestamps()).containsExactly(new AdminCurriculumDto.Timestamp(0, "Introduction"));
    }

    @Test void updatesMaterialWithoutChangingItsIdAndDeletesUnusedAssessmentChildren() {
        repository.saveContent(content(ContentKind.material, "Original"), "material-1");
        repository.saveContent(content(ContentKind.material, "Updated"), "material-1");
        assertThat(repository.contentId(chapterId, ContentKind.material)).isEqualTo("material-1");
        assertThat(repository.contents(chapterId).getFirst().title()).isEqualTo("Updated");
        repository.saveContent(content(ContentKind.pre_test, "Pre-test"), "generated");
        String testId = repository.contentId(chapterId, ContentKind.pre_test);
        repository.deleteContent(ContentKind.pre_test, testId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lms_chapter_test_options", Integer.class)).isZero();
        assertThat(repository.contentId(chapterId, ContentKind.pre_test)).isNull();
    }

    @Test void savesAllGroupsScheduleAndPreservesAccessAndDate() {
        var date = OffsetDateTime.parse("2026-10-21T09:00:00+07:00");
        var r = new AdminCurriculumRequest.Session(PROJECT, chapterId, null, null, date, 90, "Zoom", "", ChapterMethod.online, null, Status.active, true);
        Integer id = repository.saveSession(r, true);
        assertThat(repository.audienceExists(chapterId, null, null)).isTrue();
        assertThat(repository.audienceExists(chapterId, null, id)).isFalse();
        var updated = new AdminCurriculumRequest.Session(PROJECT, chapterId, id, null, date, 120, "Zoom", "", ChapterMethod.online, null, Status.active, false);
        repository.saveSession(updated, false);
        var session = repository.sessions(chapterId).getFirst();
        assertThat(session.isLocked()).isFalse();
        assertThat(session.sessionDate().toInstant()).isEqualTo(date.toInstant());
        assertThat(session.onlyGroupId()).isNull();
        assertThat(session.durationMinutes()).isEqualTo(120);
        repository.deleteSession(new AdminCurriculumRequest.SessionRef(PROJECT, chapterId, id));
        assertThat(repository.sessions(chapterId)).isEmpty();
    }

    @Test void projectScopePreventsCrossProjectChanges() {
        assertThatThrownBy(() -> repository.lockChapter("another-project", chapterId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(repository.list("another-project").chapters()).isEmpty();
    }

    @Test void progressIsDetectedAndForeignKeyPreventsDeletionEvenDuringARace() {
        repository.saveContent(content(ContentKind.material, "Lesson"), "material-1");
        jdbc.update("INSERT INTO lms_material_completions(student_access_id, material_id) VALUES ('learner', 'material-1')");
        assertThat(repository.hasProgress(ContentKind.material, "material-1")).isTrue();
        assertThatThrownBy(() -> repository.deleteContent(ContentKind.material, "material-1")).isInstanceOf(DataIntegrityViolationException.class);
    }
}
