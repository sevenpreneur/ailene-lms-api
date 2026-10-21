package com.ailene.lms.admin;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class AdminInsightsRepository {

    // Every query reads people through this CTE, so the group filter is applied in exactly one place.
    private static final String ACCESSES = """
            WITH acc AS (
                SELECT a.id AS access_id, a.role::text AS role, a.group_id, g.name AS group_name,
                       u.full_name, u.email, u.avatar
                FROM lms_accesses a
                JOIN lms_users u ON u.id = a.user_id
                LEFT JOIN lms_groups g ON g.id = a.group_id AND g.project_id = a.project_id
                WHERE a.project_id = :project
                  AND (CAST(:group AS INTEGER) IS NULL OR a.group_id = CAST(:group AS INTEGER))
            )
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminInsightsRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public int countLearners(String projectId, Integer groupId) {
        Integer count = jdbc.queryForObject(ACCESSES + "SELECT COUNT(*) FROM acc WHERE role <> 'sponsor'",
                params(projectId, groupId), Integer.class);
        return count == null ? 0 : count;
    }

    public Map<String, AdminPreAssessmentInsights.Flags> preAssessmentFlags(String projectId, Integer groupId) {
        var rows = jdbc.query(ACCESSES + """
                SELECT pa.access_id, pa.knowledge_score, pa.skills_score, pa.behaviours_score,
                       pa.security_gate_passed, pa.behaviour_gate_passed, pa.is_overclaim, pa.is_shadow_ai
                FROM lms_pa_attempts pa
                JOIN acc ON acc.access_id = pa.access_id
                WHERE pa.status = 'scored'
                """, params(projectId, groupId), (r, i) -> Map.entry(r.getString("access_id"),
                new AdminPreAssessmentInsights.Flags(dbl(r, "knowledge_score"), dbl(r, "skills_score"),
                        dbl(r, "behaviours_score"), r.getObject("security_gate_passed", Boolean.class),
                        r.getObject("behaviour_gate_passed", Boolean.class),
                        r.getObject("is_overclaim", Boolean.class), r.getObject("is_shadow_ai", Boolean.class))));
        return rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<AdminGuidedInsights.Row> guidedSessions(String projectId, Integer groupId) {
        return jdbc.query(ACCESSES + """
                SELECT gs.id, gs.status, gs.current_step, gs.daily_task, gs.frequency::text AS frequency,
                       gs.pain_point, gs.summary, gs.minutes_without_ai, gs.estimated_minutes_with_ai,
                       gs.created_at, gs.updated_at, jsonb_array_length(gs.steps) AS step_count,
                       (SELECT COUNT(*) FROM jsonb_array_elements(gs.steps) e
                        WHERE COALESCE((e->>'is_done')::boolean, false)) AS steps_done,
                       uc.name AS use_case_name, sub.submitted_at, sub.reviewed_at, sub.is_accepted,
                       sub.hours_saved, sub.helpfulness_rating, sub.testimonial, sub.comment, acc.*
                FROM lms_guided_use_case_sessions gs
                JOIN acc ON acc.access_id = gs.student_access_id
                LEFT JOIN lms_use_cases uc ON uc.id = gs.use_case_id
                LEFT JOIN lms_use_case_submissions sub ON sub.guided_session_id = gs.id
                WHERE gs.project_id = :project
                ORDER BY gs.updated_at DESC, gs.id DESC
                """, params(projectId, groupId), (r, i) -> new AdminGuidedInsights.Row(r.getInt("id"), learner(r),
                r.getString("status"), r.getInt("current_step"), reviewStatus(r), r.getString("use_case_name"),
                r.getString("daily_task"), r.getString("frequency"), r.getString("pain_point"),
                r.getString("summary"), dbl(r, "minutes_without_ai"),
                r.getObject("estimated_minutes_with_ai", Integer.class), r.getInt("step_count"),
                r.getInt("steps_done"), dbl(r, "hours_saved"), r.getObject("helpfulness_rating", Integer.class),
                r.getString("testimonial"), r.getString("comment"), time(r, "submitted_at"),
                time(r, "reviewed_at"), time(r, "created_at"), time(r, "updated_at")));
    }

    public List<AdminInsightsChapter> chapters(String projectId) {
        return jdbc.query("SELECT id, name, level_id FROM lms_chapters WHERE project_id = :project ORDER BY level_id, id",
                Map.of("project", projectId), (r, i) -> chapter(r, "id", "name"));
    }

    public List<AdminReviewInsights.Response> surveys(String projectId, Integer groupId) {
        return jdbc.query(ACCESSES + """
                SELECT s.*, c.name AS chapter_name, c.level_id, acc.*
                FROM lms_chapter_surveys s
                JOIN lms_chapters c ON c.id = s.chapter_id AND c.project_id = :project
                JOIN acc ON acc.access_id = s.student_access_id
                ORDER BY s.submitted_at DESC, s.id DESC
                """, params(projectId, groupId), (r, i) -> {
            var ratings = new AdminReviewInsights.Ratings(dbl(r, "trainer_clarity"), dbl(r, "trainer_mastery"),
                    dbl(r, "trainer_responsiveness"), dbl(r, "trainer_engagement"), dbl(r, "material_relevance"),
                    dbl(r, "material_flow"), dbl(r, "material_depth"), dbl(r, "learning_value"));
            return new AdminReviewInsights.Response(r.getInt("id"), learner(r), chapter(r, "chapter_id", "chapter_name"),
                    ratings, AdminInsightsMath.overall(ratings), r.getString("missing_topics"),
                    r.getString("favorite_material"), r.getString("disliked_material"),
                    r.getString("improvement_suggestion"), time(r, "submitted_at"));
        });
    }

    public record TestKinds(boolean hasPre, boolean hasPost) {
    }

    public Map<Integer, TestKinds> chapterTestKinds(String projectId) {
        var rows = jdbc.query("""
                SELECT c.id, bool_or(t.kind = 'pre_test') AS has_pre, bool_or(t.kind = 'post_test') AS has_post
                FROM lms_chapter_tests t JOIN lms_chapters c ON c.id = t.chapter_id
                WHERE c.project_id = :project AND t.status = 'active'
                GROUP BY c.id
                """, Map.of("project", projectId), (r, i) -> Map.entry(r.getInt("id"),
                new TestKinds(r.getBoolean("has_pre"), r.getBoolean("has_post"))));
        return rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<AdminTestInsights.Row> testResults(String projectId, Integer groupId) {
        return jdbc.query(ACCESSES + """
                SELECT c.id AS chapter_id, c.name AS chapter_name, c.level_id,
                       acc.access_id, acc.role, acc.full_name, acc.email, acc.avatar, acc.group_id, acc.group_name,
                       COUNT(*) FILTER (WHERE t.kind = 'pre_test') > 0 AS pre_submitted,
                       MAX(sub.score) FILTER (WHERE t.kind = 'pre_test') AS pre_score,
                       MAX(sub.submitted_at) FILTER (WHERE t.kind = 'pre_test') AS pre_at,
                       COUNT(*) FILTER (WHERE t.kind = 'post_test') > 0 AS post_submitted,
                       MAX(sub.score) FILTER (WHERE t.kind = 'post_test') AS post_score,
                       MAX(sub.submitted_at) FILTER (WHERE t.kind = 'post_test') AS post_at
                FROM lms_chapter_test_submissions sub
                JOIN lms_chapter_tests t ON t.id = sub.test_id
                JOIN lms_chapters c ON c.id = t.chapter_id AND c.project_id = :project
                JOIN acc ON acc.access_id = sub.student_access_id
                GROUP BY c.id, c.name, c.level_id, acc.access_id, acc.role, acc.full_name, acc.email, acc.avatar,
                         acc.group_id, acc.group_name
                ORDER BY c.level_id, c.id, acc.full_name
                """, params(projectId, groupId), (r, i) -> {
            Integer pre = r.getObject("pre_score", Integer.class);
            Integer post = r.getObject("post_score", Integer.class);
            return new AdminTestInsights.Row(learner(r), chapter(r, "chapter_id", "chapter_name"),
                    r.getBoolean("pre_submitted"), pre, time(r, "pre_at"), r.getBoolean("post_submitted"), post,
                    time(r, "post_at"), pre != null && post != null ? post - pre : null);
        });
    }

    public List<AdminQuizInsights.QuizRow> quizzes(String projectId) {
        return jdbc.query("""
                SELECT q.id, q.name, c.id AS chapter_id, c.name AS chapter_name, c.level_id,
                       (SELECT COUNT(*) FROM lms_quiz_questions qq WHERE qq.quiz_id = q.id) AS question_count
                FROM lms_quizzes q JOIN lms_chapters c ON c.id = q.chapter_id
                WHERE c.project_id = :project
                ORDER BY c.level_id, c.id
                """, Map.of("project", projectId), (r, i) -> new AdminQuizInsights.QuizRow(r.getString("id"),
                r.getString("name"), chapter(r, "chapter_id", "chapter_name"), r.getInt("question_count"), 0, 0,
                null, null, null));
    }

    public List<AdminQuizInsights.Submission> quizSubmissions(String projectId, Integer groupId) {
        return jdbc.query(ACCESSES + """
                SELECT qs.id, qs.quiz_id, q.name AS quiz_name, c.id AS chapter_id, c.name AS chapter_name, c.level_id,
                       qs.attempt_number, qs.score, qs.started_at, qs.submitted_at, acc.*
                FROM lms_quiz_submissions qs
                JOIN lms_quizzes q ON q.id = qs.quiz_id
                JOIN lms_chapters c ON c.id = q.chapter_id AND c.project_id = :project
                JOIN acc ON acc.access_id = qs.student_access_id
                WHERE qs.is_completed
                ORDER BY qs.submitted_at DESC, qs.id DESC
                """, params(projectId, groupId), (r, i) -> new AdminQuizInsights.Submission(r.getInt("id"),
                learner(r), r.getString("quiz_id"), r.getString("quiz_name"), chapter(r, "chapter_id", "chapter_name"),
                r.getInt("attempt_number"), r.getInt("score"), time(r, "started_at"), time(r, "submitted_at")));
    }

    private static MapSqlParameterSource params(String projectId, Integer groupId) {
        return new MapSqlParameterSource().addValue("project", projectId).addValue("group", groupId);
    }

    private static AdminInsightsLearner learner(ResultSet r) throws SQLException {
        return new AdminInsightsLearner(r.getString("access_id"), r.getString("role"), r.getString("full_name"),
                r.getString("email"), r.getString("avatar"), r.getObject("group_id", Integer.class),
                r.getString("group_name"));
    }

    private static AdminInsightsChapter chapter(ResultSet r, String id, String name) throws SQLException {
        return new AdminInsightsChapter(r.getInt(id), r.getString(name), r.getInt("level_id"));
    }

    // A champion can send a submission back; it counts as waiting again only once the learner resubmits after that.
    private static AdminGuidedInsights.ReviewStatus reviewStatus(ResultSet r) throws SQLException {
        OffsetDateTime submitted = time(r, "submitted_at");
        OffsetDateTime reviewed = time(r, "reviewed_at");
        if (r.getBoolean("is_accepted")) return AdminGuidedInsights.ReviewStatus.accepted;
        if (reviewed != null && (submitted == null || !submitted.isAfter(reviewed)))
            return AdminGuidedInsights.ReviewStatus.needs_revision;
        if (submitted != null) return AdminGuidedInsights.ReviewStatus.awaiting_review;
        return AdminGuidedInsights.ReviewStatus.in_progress;
    }

    private static OffsetDateTime time(ResultSet r, String column) throws SQLException {
        return r.getObject(column, OffsetDateTime.class);
    }

    private static Double dbl(ResultSet r, String column) throws SQLException {
        BigDecimal value = r.getBigDecimal(column);
        return value == null ? null : value.doubleValue();
    }
}
