package com.ailene.lms.admin;

import com.ailene.lms.admin.AdminCurriculumDto.*;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class AdminCurriculumRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AdminCurriculumRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public AdminCurriculumDto list(String projectId) {
        var levels = jdbc.query("SELECT id, name FROM lms_levels ORDER BY level_number", Map.of(),
                (r, i) -> new Level(r.getInt("id"), r.getString("name")));
        var chapters = jdbc.query("SELECT * FROM lms_chapters WHERE project_id = :project ORDER BY level_id, id",
                Map.of("project", projectId), (r, i) -> new Chapter(r.getInt("id"), r.getInt("level_id"),
                        r.getString("name"), r.getString("description"), r.getString("status"),
                        sessions(r.getInt("id")), contents(r.getInt("id"))));
        return new AdminCurriculumDto(levels, chapters);
    }

    public void lockChapter(String projectId, Integer chapterId) {
        var rows = jdbc.queryForList("SELECT id FROM lms_chapters WHERE id = :id AND project_id = :project FOR UPDATE",
                Map.of("id", chapterId, "project", projectId));
        if (rows.isEmpty()) throw new ResourceNotFoundException("Session not found in this project");
    }

    public boolean levelExists(Integer levelId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lms_levels WHERE id = :id)",
                Map.of("id", levelId), Boolean.class));
    }

    public Integer createChapter(AdminCurriculumRequest.Chapter r) {
        return jdbc.queryForObject("""
                INSERT INTO lms_chapters(project_id, level_id, name, description, status)
                VALUES(:project, :level, :name, :description, CAST(:status AS status_enum)) RETURNING id
                """, chapterParams(r), Integer.class);
    }

    public void updateChapter(AdminCurriculumRequest.Chapter r) {
        jdbc.update("""
                UPDATE lms_chapters SET level_id = :level, name = :name, description = :description,
                status = CAST(:status AS status_enum), updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND project_id = :project
                """, chapterParams(r));
    }

    private MapSqlParameterSource chapterParams(AdminCurriculumRequest.Chapter r) {
        return new MapSqlParameterSource().addValue("project", r.projectId()).addValue("id", r.chapterId())
                .addValue("level", r.levelId()).addValue("name", r.name().trim())
                .addValue("description", r.description()).addValue("status", r.status().name());
    }

    public List<Session> sessions(Integer chapterId) {
        return jdbc.query("SELECT * FROM lms_chapter_sessions WHERE chapter_id = :chapter ORDER BY session_date, id",
                Map.of("chapter", chapterId), (r, i) -> new Session(r.getInt("id"),
                        r.getObject("only_group_id", Integer.class), r.getObject("session_date", OffsetDateTime.class),
                        r.getInt("duration_minutes"), r.getString("location_name"), r.getString("location_url"),
                        r.getString("method"), r.getString("trainer_id"), r.getString("status"), r.getBoolean("is_locked")));
    }

    public boolean groupExists(String projectId, Integer groupId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lms_groups WHERE id = :id AND project_id = :project)",
                Map.of("id", groupId, "project", projectId), Boolean.class));
    }

    public boolean audienceExists(Integer chapterId, Integer groupId, Integer excluding) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM lms_chapter_sessions WHERE chapter_id = :chapter
                AND only_group_id IS NOT DISTINCT FROM CAST(:group AS INTEGER)
                AND (:excluding IS NULL OR id <> CAST(:excluding AS INTEGER)))
                """, new MapSqlParameterSource("chapter", chapterId).addValue("group", groupId)
                .addValue("excluding", excluding, java.sql.Types.INTEGER), Boolean.class));
    }

    public Integer saveSession(AdminCurriculumRequest.Session r, boolean creating) {
        var p = new MapSqlParameterSource("chapter", r.chapterId()).addValue("project", r.projectId())
                .addValue("id", r.sessionId()).addValue("group", r.onlyGroupId()).addValue("date", r.sessionDate())
                .addValue("duration", r.durationMinutes()).addValue("location", r.locationName().trim())
                .addValue("url", r.locationUrl() == null ? "" : r.locationUrl()).addValue("method", r.method().name())
                .addValue("trainer", r.trainerId()).addValue("status", r.status().name()).addValue("locked", r.isLocked());
        if (creating) return jdbc.queryForObject("""
                INSERT INTO lms_chapter_sessions(chapter_id, project_id, only_group_id, session_date, duration_minutes,
                location_name, location_url, method, trainer_id, status, is_locked)
                VALUES(:chapter, :project, :group, :date, :duration, :location, :url,
                CAST(:method AS lms_chapter_method_enum), :trainer, CAST(:status AS status_enum), :locked) RETURNING id
                """, p, Integer.class);
        int count = jdbc.update("""
                UPDATE lms_chapter_sessions SET only_group_id = :group, session_date = :date, duration_minutes = :duration,
                location_name = :location, location_url = :url, method = CAST(:method AS lms_chapter_method_enum),
                trainer_id = :trainer, status = CAST(:status AS status_enum), is_locked = :locked, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND chapter_id = :chapter AND project_id = :project
                """, p);
        if (count == 0) throw new ResourceNotFoundException("Schedule not found in this session");
        return r.sessionId();
    }

    public void deleteSession(AdminCurriculumRequest.SessionRef r) {
        int count = jdbc.update("DELETE FROM lms_chapter_sessions WHERE id = :id AND chapter_id = :chapter AND project_id = :project",
                Map.of("id", r.sessionId(), "chapter", r.chapterId(), "project", r.projectId()));
        if (count == 0) throw new ResourceNotFoundException("Schedule not found in this session");
    }

    public List<Content> contents(Integer chapterId) {
        var result = new ArrayList<Content>();
        for (ContentKind kind : ContentKind.values()) {
            String table = table(kind);
            String title = kind == ContentKind.material || kind == ContentKind.video ? "title" : "name";
            String extra = switch (kind) {
                case material -> "content, file_url, image_url, NULL AS video_url, xp_reward";
                case video -> "NULL AS content, NULL AS file_url, NULL AS image_url, video_url, xp_reward";
                default -> "NULL AS content, NULL AS file_url, NULL AS image_url, NULL AS video_url, 0 AS xp_reward";
            };
            result.addAll(jdbc.query("SELECT id::text AS id, " + title + " AS title, description, status, " + extra
                            + " FROM " + table + " WHERE chapter_id = :chapter" + testCondition(kind),
                    Map.of("chapter", chapterId), (r, i) -> content(r, kind)));
        }
        return result;
    }

    private Content content(ResultSet r, ContentKind kind) throws SQLException {
        String id = r.getString("id");
        boolean assessment = isAssessment(kind);
        var questions = assessment ? questions(kind, id) : List.<Question>of();
        var timestamps = kind == ContentKind.video ? jdbc.query("SELECT start_seconds, title FROM lms_video_timestamps WHERE video_id::text = :id ORDER BY start_seconds",
                Map.of("id", id), (row, i) -> new Timestamp(row.getInt(1), row.getString(2))) : List.<Timestamp>of();
        return new Content(id, kind, r.getString("title"), r.getString("description"), r.getString("status"),
                r.getString("content"), r.getString("file_url"), r.getString("image_url"), r.getString("video_url"),
                r.getInt("xp_reward"), questions, timestamps, hasProgress(kind, id));
    }

    private List<Question> questions(ContentKind kind, String id) {
        boolean quiz = kind == ContentKind.quiz;
        String table = quiz ? "lms_quiz_questions" : "lms_chapter_test_questions";
        return jdbc.query("SELECT id, " + (quiz ? "'multiple_choice' AS type, xp_reward" : "type::text, 0 AS xp_reward")
                        + ", question, explanation FROM " + table + " WHERE " + (quiz ? "quiz_id" : "test_id::text") + " = :id ORDER BY order_index, id",
                Map.of("id", id), (r, i) -> new Question(r.getInt("id"), r.getString("type"), r.getString("question"),
                        r.getString("explanation"), r.getInt("xp_reward"), jdbc.query("SELECT option_code, text, is_correct FROM "
                        + (quiz ? "lms_quiz_options" : "lms_chapter_test_options") + " WHERE question_id = :id ORDER BY option_code",
                        Map.of("id", r.getInt("id")), (o, n) -> new Option(o.getString(1), o.getString(2), o.getBoolean(3)))));
    }

    public String contentId(Integer chapterId, ContentKind kind) {
        return jdbc.query("SELECT id::text FROM " + table(kind) + " WHERE chapter_id = :chapter" + testCondition(kind),
                Map.of("chapter", chapterId), (r, i) -> r.getString(1)).stream().findFirst().orElse(null);
    }

    public boolean hasProgress(ContentKind kind, String id) {
        String table = switch (kind) {
            case material -> "lms_material_completions";
            case video -> "lms_video_completions";
            case quiz -> "lms_quiz_submissions";
            default -> "lms_chapter_test_submissions";
        };
        String column = switch (kind) { case material -> "material_id"; case video -> "video_id"; case quiz -> "quiz_id"; default -> "test_id"; };
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " + table + " WHERE " + column + "::text = :id)",
                Map.of("id", id), Boolean.class));
    }

    public void saveContent(AdminCurriculumRequest.Content r, String id) {
        String title = r.kind() == ContentKind.material || r.kind() == ContentKind.video ? "title" : "name";
        var p = new MapSqlParameterSource("chapter", r.chapterId()).addValue("title", r.title().trim())
                .addValue("description", r.description()).addValue("status", r.status().name()).addValue("id", id)
                .addValue("content", r.content()).addValue("file", r.fileUrl()).addValue("image", r.imageUrl())
                .addValue("video", r.videoUrl()).addValue("xp", r.xpReward());
        String existing = contentId(r.chapterId(), r.kind());
        if (existing == null) {
            String columns = "chapter_id, " + title + ", description, status";
            String values = ":chapter, :title, :description, CAST(:status AS status_enum)";
            switch (r.kind()) {
                case material -> { columns += ", id, content, file_url, image_url, xp_reward"; values += ", :id, :content, :file, :image, :xp"; }
                case video -> { columns += ", video_url, xp_reward"; values += ", :video, :xp"; }
                case quiz -> { columns += ", id"; values += ", :id"; }
                default -> { columns += ", kind"; values += ", CAST('" + r.kind().name() + "' AS lms_chapter_test_kind_enum)"; }
            }
            id = jdbc.queryForObject("INSERT INTO " + table(r.kind()) + "(" + columns + ") VALUES(" + values + ") RETURNING id::text", p, String.class);
        } else {
            String extra = switch (r.kind()) {
                case material -> ", content = :content, file_url = :file, image_url = :image, xp_reward = :xp";
                case video -> ", video_url = :video, xp_reward = :xp";
                default -> "";
            };
            jdbc.update("UPDATE " + table(r.kind()) + " SET " + title + " = :title, description = :description, status = CAST(:status AS status_enum), updated_at = CURRENT_TIMESTAMP"
                    + extra + " WHERE id::text = :id", p);
        }
        if (isAssessment(r.kind())) {
            deleteQuestions(r.kind(), id);
            int order = 0;
            for (var q : r.questions()) {
                boolean quiz = r.kind() == ContentKind.quiz;
                var params = new MapSqlParameterSource("id", id).addValue("question", q.question().trim())
                        .addValue("explanation", q.explanation()).addValue("order", order++).addValue("xp", q.xpReward()).addValue("type", q.type());
                Integer questionId = jdbc.queryForObject(quiz ? """
                        INSERT INTO lms_quiz_questions(quiz_id, question, explanation, order_index, xp_reward)
                        VALUES(:id, :question, :explanation, :order, :xp) RETURNING id
                        """ : """
                        INSERT INTO lms_chapter_test_questions(test_id, type, question, explanation, order_index)
                        VALUES(CAST(:id AS INTEGER), CAST(:type AS lms_chapter_test_question_type_enum), :question, :explanation, :order) RETURNING id
                        """, params, Integer.class);
                for (var option : q.options()) jdbc.update("INSERT INTO " + (quiz ? "lms_quiz_options" : "lms_chapter_test_options")
                                + "(question_id, option_code, text, is_correct) VALUES(:id, :code, :text, :correct)",
                        Map.of("id", questionId, "code", option.optionCode(), "text", option.text().trim(), "correct", option.isCorrect()));
            }
        }
        if (r.kind() == ContentKind.video) {
            jdbc.update("DELETE FROM lms_video_timestamps WHERE video_id::text = :id", Map.of("id", id));
            for (var t : r.timestamps()) jdbc.update("INSERT INTO lms_video_timestamps(video_id, start_seconds, title) VALUES(CAST(:id AS INTEGER), :seconds, :title)",
                    Map.of("id", id, "seconds", t.startSeconds(), "title", t.title().trim()));
        }
    }

    private void deleteQuestions(ContentKind kind, String id) {
        boolean quiz = kind == ContentKind.quiz;
        String questions = quiz ? "lms_quiz_questions" : "lms_chapter_test_questions";
        String options = quiz ? "lms_quiz_options" : "lms_chapter_test_options";
        String fk = quiz ? "quiz_id" : "test_id::text";
        jdbc.update("DELETE FROM " + options + " WHERE question_id IN(SELECT id FROM " + questions + " WHERE " + fk + " = :id)", Map.of("id", id));
        jdbc.update("DELETE FROM " + questions + " WHERE " + fk + " = :id", Map.of("id", id));
    }

    public void deleteContent(ContentKind kind, String id) {
        if (isAssessment(kind)) deleteQuestions(kind, id);
        if (kind == ContentKind.video) jdbc.update("DELETE FROM lms_video_timestamps WHERE video_id::text = :id", Map.of("id", id));
        jdbc.update("DELETE FROM " + table(kind) + " WHERE id::text = :id", Map.of("id", id));
    }

    public void deleteChapter(String projectId, Integer chapterId) {
        var p = Map.of("project", projectId, "chapter", chapterId);
        jdbc.update("DELETE FROM lms_chapter_sessions WHERE chapter_id = :chapter AND project_id = :project", p);
        jdbc.update("DELETE FROM lms_chapters WHERE id = :chapter AND project_id = :project", p);
    }

    static boolean isAssessment(ContentKind kind) { return kind == ContentKind.quiz || kind == ContentKind.pre_test || kind == ContentKind.post_test; }
    private static String table(ContentKind kind) {
        return switch (kind) { case material -> "lms_materials"; case video -> "lms_videos"; case quiz -> "lms_quizzes"; default -> "lms_chapter_tests"; };
    }
    private static String testCondition(ContentKind kind) {
        return kind == ContentKind.pre_test || kind == ContentKind.post_test ? " AND kind = '" + kind.name() + "'" : "";
    }
}
