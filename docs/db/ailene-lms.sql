-- PostgreSQL Database for Ailene LMS

------------------
-- Enumerations --
------------------

CREATE TYPE status_enum AS ENUM (
  'active',
  'inactive'
);

-- Enumeration for the lms_chapters table

CREATE TYPE lms_chapter_method_enum AS ENUM (
  'online',
  'offline'
);

-- Enumeration for the lms_chapter_trainer_requests table

CREATE TYPE lms_chapter_trainer_request_status_enum AS ENUM (
  'pending',
  'selected',
  'rejected'
);

-- Enumeration for the lms_accesses table

CREATE TYPE lms_access_role_enum AS ENUM (
  'champion',
  'student',
  'sponsor'
);

-- Enumeration for the lms_xp_earnings table

CREATE TYPE lms_learning_type_enum AS ENUM (
  'quiz',
  'video',
  'material',
  'use_case',
  'prompt'
);

-- Enumeration for the lms_use_case_submissions table

CREATE TYPE lms_use_case_frequency_enum AS ENUM (
  'daily',
  'weekly',
  'monthly',
  'occasionally'
);

CREATE TYPE lms_use_case_type_enum AS ENUM (
  'workflow_automation',
  'content_creation',
  'data_analysis',
  'research',
  'communication',
  'decision_support',
  'learning',
  'other'
);

-- Enumeration for the lms_pre_assessments table

CREATE TYPE lms_pa_ai_use_freq_enum AS ENUM (
  'never',
  'tried',
  'weekly',
  'daily',
  'intensive'
);

CREATE TYPE lms_pa_output_review_enum AS ENUM (
  'no_check',
  'sometimes',
  'always',
  'cross_check',
  'no_use'
);

CREATE TYPE lms_pa_team_adoption_enum AS ENUM (
  'none',
  'personal',
  'pilot',
  'policy',
  'integrated'
);

CREATE TYPE lms_pa_frequency_enum AS ENUM (
  'never',
  'rarely',
  'sometimes',
  'often',
  'always'
);

CREATE TYPE lms_pa_prompt_skill_enum AS ENUM (
  'none',
  'basic',
  'decent',
  'structured',
  'expert'
);

CREATE TYPE lms_pa_refine_scenario_enum AS ENUM (
  'targeted',
  'switch_tool',
  'manual',
  'restart'
);

CREATE TYPE lms_pa_attitude_enum AS ENUM (
  'too_risky',
  'cautious',
  'neutral',
  'supportive',
  'essential'
);

CREATE TYPE lms_pa_motivation_enum AS ENUM (
  'mandatory',
  'curious',
  'tentative',
  'ready',
  'eager'
);

------------
-- Tables --
------------

-- Program Structure
--
-- trainers is owned by a separate app (not the LMS flow) and referenced but not defined in this file.

CREATE TABLE lms_projects (
  id             CHAR(21)     PRIMARY KEY,
  name           VARCHAR      NOT NULL,
  company_id     INTEGER          NULL,
  created_at     TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  attendee_pax   INTEGER          NULL,
  pipeline_id    INTEGER      NOT NULL
);

CREATE TABLE lms_levels (
  id            SERIAL       PRIMARY KEY,
  project_id    CHAR(21)     NOT NULL,
  level_number  SMALLINT     NOT NULL  UNIQUE,
  name          VARCHAR      NOT NULL,
  status        status_enum  NOT NULL  DEFAULT 'active',
  created_at    TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (id, project_id)
);

-- Contents

CREATE TABLE lms_chapters (
  id                 SERIAL                    PRIMARY KEY,
  level_id           INTEGER                   NOT NULL,
  name               VARCHAR                   NOT NULL,
  description        TEXT                          NULL,
  session_date       TIMESTAMPTZ               NOT NULL,
  status             status_enum               NOT NULL  DEFAULT 'active',
  created_at         TIMESTAMPTZ               NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ               NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  trainer_id         CHAR(21)                      NULL,
  duration_minutes   INTEGER                   NOT NULL,
  location_name      VARCHAR                   NOT NULL,
  location_url       VARCHAR                   NOT NULL,
  method             lms_chapter_method_enum   NOT NULL  DEFAULT 'offline'
);

CREATE TABLE lms_chapter_trainer_requests (
  id           SERIAL                                     PRIMARY KEY,
  chapter_id   INTEGER                                     NOT NULL,
  trainer_id   CHAR(21)                                    NOT NULL,
  status       lms_chapter_trainer_request_status_enum     NOT NULL  DEFAULT 'pending',
  reviewed_by  UUID                                            NULL,
  reviewed_at  TIMESTAMPTZ                                     NULL,
  created_at   TIMESTAMPTZ                                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ                                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (chapter_id, trainer_id)
);

CREATE TABLE lms_materials (
  id           VARCHAR      PRIMARY KEY,
  chapter_id   INTEGER      NOT NULL,
  title        VARCHAR      NOT NULL,
  description  TEXT             NULL,
  content      TEXT             NULL,
  file_url     TEXT             NULL,
  image_url    TEXT             NULL,
  xp_reward    SMALLINT     NOT NULL  DEFAULT 0,
  order_index  SMALLINT     NOT NULL  DEFAULT 0,
  status       status_enum  NOT NULL  DEFAULT 'active',
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_videos (
  id           SERIAL       PRIMARY KEY,
  chapter_id   INTEGER      NOT NULL,
  title        VARCHAR      NOT NULL,
  description  TEXT             NULL,
  video_url    TEXT         NOT NULL,
  xp_reward    SMALLINT     NOT NULL  DEFAULT 0,
  order_index  SMALLINT     NOT NULL  DEFAULT 0,
  status       status_enum  NOT NULL  DEFAULT 'active',
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_quizzes (
  id           VARCHAR      PRIMARY KEY,
  chapter_id   INTEGER      NOT NULL,
  name         VARCHAR      NOT NULL,
  description  TEXT             NULL,
  order_index  SMALLINT     NOT NULL  DEFAULT 0,
  status       status_enum  NOT NULL  DEFAULT 'active',
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_quiz_questions (
  id           SERIAL       PRIMARY KEY,
  quiz_id      VARCHAR      NOT NULL,
  question     TEXT         NOT NULL,
  explanation  TEXT             NULL,
  order_index  SMALLINT     NOT NULL  DEFAULT 0,
  xp_reward    SMALLINT     NOT NULL  DEFAULT 0,
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_quiz_options (
  id           SERIAL   PRIMARY KEY,
  question_id  INTEGER  NOT NULL,
  option_code  VARCHAR  NOT NULL,
  text         VARCHAR  NOT NULL,
  is_correct   BOOLEAN  NOT NULL  DEFAULT FALSE
);

-- Lookup

CREATE TABLE lms_categories (
  id    SMALLSERIAL  PRIMARY KEY,
  name  VARCHAR      NOT NULL  UNIQUE
);

-- Practical Learning

CREATE TABLE lms_prompts (
  id               SERIAL       PRIMARY KEY,
  level_id         INTEGER      NOT NULL,
  name             VARCHAR      NOT NULL,
  scenario         TEXT         NOT NULL,
  expected_output  TEXT         NOT NULL,
  xp_reward        SMALLINT     NOT NULL  DEFAULT 70,
  status           status_enum  NOT NULL  DEFAULT 'active',
  is_self_created  BOOLEAN      NOT NULL  DEFAULT FALSE,
  created_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_prompt_categories (
  prompt_id    INTEGER   NOT NULL,
  category_id  SMALLINT  NOT NULL,
  PRIMARY KEY (prompt_id, category_id)
);

CREATE TABLE lms_use_cases (
  id               SERIAL       PRIMARY KEY,
  level_id         INTEGER      NOT NULL,
  name             VARCHAR      NOT NULL,
  description      TEXT         NOT NULL,
  xp_reward        SMALLINT     NOT NULL  DEFAULT 70,
  status           status_enum  NOT NULL  DEFAULT 'active',
  is_self_created  BOOLEAN      NOT NULL  DEFAULT FALSE,
  created_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_use_case_categories (
  use_case_id  INTEGER   NOT NULL,
  category_id  SMALLINT  NOT NULL,
  PRIMARY KEY (use_case_id, category_id)
);

-- Users

CREATE TABLE lms_users (
  id              UUID         PRIMARY KEY,
  full_name       VARCHAR      NOT NULL,
  email           VARCHAR      NOT NULL  UNIQUE,
  avatar          VARCHAR          NULL,
  job_title       VARCHAR      NOT NULL,
  last_active_at  TIMESTAMPTZ      NULL,
  created_at      TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_tokens (
  id           SERIAL       PRIMARY KEY,
  user_id      UUID         NOT NULL,
  token        TEXT         NOT NULL  UNIQUE,
  is_active    BOOLEAN      NOT NULL  DEFAULT FALSE,
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_groups (
  id           SERIAL       PRIMARY KEY,
  name         VARCHAR      NOT NULL,
  project_id   CHAR(21)     NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (id, project_id)
);

CREATE TABLE lms_accesses (
  id                 CHAR(21)              PRIMARY KEY,
  project_id         CHAR(21)              NOT NULL,
  user_id            UUID                  NOT NULL,
  group_id           INTEGER                   NULL,
  current_level_id   INTEGER                   NULL,
  role               lms_access_role_enum  NOT NULL,
  created_at         TIMESTAMPTZ           NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ           NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (project_id, user_id)
);

-- Submissions & Progress

CREATE TABLE lms_level_history (
  access_id   CHAR(21)     NOT NULL,
  level_id    INTEGER      NOT NULL,
  reached_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (access_id, level_id)
);

CREATE TABLE lms_coaching_notes (
  id                   SERIAL       PRIMARY KEY,
  student_access_id    CHAR(21)     NOT NULL,
  champion_access_id   CHAR(21)     NOT NULL,
  text                 TEXT         NOT NULL,
  created_at           TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_material_completions (
  student_access_id  CHAR(21)     NOT NULL,
  material_id         VARCHAR      NOT NULL,
  completed_at        TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (student_access_id, material_id)
);

CREATE TABLE lms_video_completions (
  student_access_id  CHAR(21)     NOT NULL,
  video_id            INTEGER      NOT NULL,
  completed_at        TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (student_access_id, video_id)
);

CREATE TABLE lms_quiz_submissions (
  id                 SERIAL       PRIMARY KEY,
  student_access_id  CHAR(21)     NOT NULL,
  quiz_id            VARCHAR      NOT NULL,
  attempt_number     SMALLINT     NOT NULL,
  answers            JSONB        NOT NULL,
  score              SMALLINT     NOT NULL,
  is_completed       BOOLEAN      NOT NULL  DEFAULT FALSE,
  started_at         TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  submitted_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, quiz_id, attempt_number)
);

CREATE TABLE lms_prompt_submissions (
  id                      SERIAL       PRIMARY KEY,
  student_access_id       CHAR(21)     NOT NULL,
  prompt_id                INTEGER      NOT NULL,
  assigned_by_access_id   CHAR(21)         NULL,
  deadline                 TIMESTAMPTZ      NULL,
  message                  TEXT             NULL,
  input                    TEXT             NULL,
  output                   TEXT             NULL,
  submitted_at             TIMESTAMPTZ      NULL,
  reviewed_by_access_id   CHAR(21)         NULL,
  reviewed_at              TIMESTAMPTZ      NULL,
  comment                  TEXT             NULL,
  is_accepted              BOOLEAN      NOT NULL  DEFAULT FALSE,
  rubric_specificity       SMALLINT         NULL,
  rubric_context           SMALLINT         NULL,
  rubric_constraints       SMALLINT         NULL,
  rubric_examples          SMALLINT         NULL,
  rubric_iteration         SMALLINT         NULL,
  created_at               TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, prompt_id)
);

CREATE TABLE lms_use_case_submissions (
  id                      SERIAL                        PRIMARY KEY,
  student_access_id       CHAR(21)                      NOT NULL,
  use_case_id              INTEGER                       NOT NULL,
  assigned_by_access_id   CHAR(21)                          NULL,
  deadline                 TIMESTAMPTZ                       NULL,
  message                  TEXT                              NULL,
  outcome_proof            VARCHAR                           NULL,
  hours_with_ai            NUMERIC(6,2)                      NULL,
  hours_without_ai         NUMERIC(6,2)                      NULL,
  description              TEXT                              NULL,
  ai_tool                  VARCHAR                           NULL,
  frequency                lms_use_case_frequency_enum       NULL,
  type                     lms_use_case_type_enum            NULL,
  submitted_at             TIMESTAMPTZ                       NULL,
  reviewed_by_access_id   CHAR(21)                          NULL,
  reviewed_at              TIMESTAMPTZ                       NULL,
  comment                  TEXT                              NULL,
  is_accepted              BOOLEAN                       NOT NULL  DEFAULT FALSE,
  created_at               TIMESTAMPTZ                   NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMPTZ                   NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, use_case_id)
);

CREATE TABLE lms_xp_earnings (
  id                  SERIAL                   PRIMARY KEY,
  student_access_id  CHAR(21)                 NOT NULL,
  learning_type       lms_learning_type_enum   NOT NULL,
  learning_id          VARCHAR                  NOT NULL,
  xp_earned            SMALLINT                 NOT NULL,
  earned_at            TIMESTAMPTZ              NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, learning_type, learning_id)
);

-- LMS pre-assessment

CREATE TABLE lms_pre_assessments (
  id                      SERIAL                          PRIMARY KEY,
  access_id               CHAR(21)                        NOT NULL  UNIQUE,
  ai_use_frequency        lms_pa_ai_use_freq_enum          NOT NULL,
  ai_tools_used           TEXT[]                               NULL,
  ai_limitations          TEXT[]                               NULL,
  output_review           lms_pa_output_review_enum       NOT NULL,
  use_cases               TEXT[]                               NULL,
  team_adoption           lms_pa_team_adoption_enum       NOT NULL,
  concrete_example        VARCHAR                              NULL,
  model_selection         lms_pa_frequency_enum           NOT NULL,
  multimodal_use          lms_pa_frequency_enum           NOT NULL,
  workflow_reuse          lms_pa_frequency_enum           NOT NULL,
  prompt_comfort          lms_pa_prompt_skill_enum        NOT NULL,
  prompt_iteration        lms_pa_frequency_enum           NOT NULL,
  refine_scenario         lms_pa_refine_scenario_enum     NOT NULL,
  professional_attitude   lms_pa_attitude_enum            NOT NULL,
  data_safety_check       lms_pa_frequency_enum           NOT NULL,
  publish_unchecked       lms_pa_frequency_enum           NOT NULL,
  biggest_challenge       TEXT                            NOT NULL,
  training_expectation    TEXT                            NOT NULL,
  motivation              lms_pa_motivation_enum          NOT NULL,
  created_at              TIMESTAMPTZ                     NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_pre_assessment_reports (
  id                  SERIAL       PRIMARY KEY,
  pre_assessment_id   INTEGER      NOT NULL  UNIQUE,
  status              VARCHAR      NOT NULL  DEFAULT 'pending',
  recommendations     JSONB            NULL,
  error_message       TEXT             NULL,
  queued_at           TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  generated_at        TIMESTAMPTZ      NULL,
  updated_at          TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

-- Miscellaneous

CREATE TABLE lms_announcement (
  id          SERIAL       PRIMARY KEY,
  project_id  CHAR(21)     NOT NULL  UNIQUE,
  title       VARCHAR      NOT NULL,
  callout     VARCHAR          NULL,
  status      status_enum  NOT NULL,
  start_date  TIMESTAMPTZ  NOT NULL,
  end_date    TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

----------------
-- References --
----------------

-- LMS program structure

ALTER TABLE lms_projects
  ADD FOREIGN KEY (company_id)  REFERENCES b2b_company (id),
  ADD FOREIGN KEY (pipeline_id) REFERENCES b2b_pipeline (id);

ALTER TABLE lms_levels
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_chapters
  ADD FOREIGN KEY (level_id)   REFERENCES lms_levels (id),
  ADD FOREIGN KEY (trainer_id) REFERENCES trainers (id);

ALTER TABLE lms_chapter_trainer_requests
  ADD FOREIGN KEY (chapter_id)  REFERENCES lms_chapters (id),
  ADD FOREIGN KEY (trainer_id)  REFERENCES trainers (id),
  ADD FOREIGN KEY (reviewed_by) REFERENCES users (id);

-- LMS content

ALTER TABLE lms_materials
  ADD FOREIGN KEY (chapter_id) REFERENCES lms_chapters (id);

ALTER TABLE lms_videos
  ADD FOREIGN KEY (chapter_id) REFERENCES lms_chapters (id);

ALTER TABLE lms_quizzes
  ADD FOREIGN KEY (chapter_id) REFERENCES lms_chapters (id);

ALTER TABLE lms_quiz_questions
  ADD FOREIGN KEY (quiz_id) REFERENCES lms_quizzes (id);

ALTER TABLE lms_quiz_options
  ADD FOREIGN KEY (question_id) REFERENCES lms_quiz_questions (id);

-- LMS categories, prompts & use cases

ALTER TABLE lms_prompts
  ADD FOREIGN KEY (level_id) REFERENCES lms_levels (id);

ALTER TABLE lms_prompt_categories
  ADD FOREIGN KEY (prompt_id)   REFERENCES lms_prompts (id),
  ADD FOREIGN KEY (category_id) REFERENCES lms_categories (id);

ALTER TABLE lms_use_cases
  ADD FOREIGN KEY (level_id) REFERENCES lms_levels (id);

ALTER TABLE lms_use_case_categories
  ADD FOREIGN KEY (use_case_id) REFERENCES lms_use_cases (id),
  ADD FOREIGN KEY (category_id) REFERENCES lms_categories (id);

-- LMS users & progress

ALTER TABLE lms_tokens
  ADD FOREIGN KEY (user_id) REFERENCES lms_users (id);

ALTER TABLE lms_groups
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_accesses
  ADD FOREIGN KEY (project_id)                   REFERENCES lms_projects (id),
  ADD FOREIGN KEY (user_id)                      REFERENCES lms_users (id),
  ADD FOREIGN KEY (group_id, project_id)         REFERENCES lms_groups (id, project_id),
  ADD FOREIGN KEY (current_level_id, project_id) REFERENCES lms_levels (id, project_id);

ALTER TABLE lms_level_history
  ADD FOREIGN KEY (access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (level_id)  REFERENCES lms_levels (id);

ALTER TABLE lms_coaching_notes
  ADD FOREIGN KEY (student_access_id)  REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (champion_access_id) REFERENCES lms_accesses (id);

ALTER TABLE lms_material_completions
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (material_id)       REFERENCES lms_materials (id);

ALTER TABLE lms_video_completions
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (video_id)          REFERENCES lms_videos (id);

ALTER TABLE lms_quiz_submissions
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (quiz_id)           REFERENCES lms_quizzes (id);

ALTER TABLE lms_prompt_submissions
  ADD FOREIGN KEY (student_access_id)     REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (prompt_id)             REFERENCES lms_prompts (id),
  ADD FOREIGN KEY (assigned_by_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (reviewed_by_access_id) REFERENCES lms_accesses (id);

ALTER TABLE lms_use_case_submissions
  ADD FOREIGN KEY (student_access_id)     REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (use_case_id)           REFERENCES lms_use_cases (id),
  ADD FOREIGN KEY (assigned_by_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (reviewed_by_access_id) REFERENCES lms_accesses (id);

ALTER TABLE lms_xp_earnings
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id);

-- LMS pre-assessment

ALTER TABLE lms_pre_assessments
  ADD FOREIGN KEY (access_id) REFERENCES lms_accesses (id);

ALTER TABLE lms_pre_assessment_reports
  ADD FOREIGN KEY (pre_assessment_id) REFERENCES lms_pre_assessments (id);

-- LMS misc

ALTER TABLE lms_announcement
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

-------------
-- Indexes --
-------------

-- LMS program structure

CREATE INDEX idx_lms_projects_company_id             ON lms_projects (company_id);
CREATE INDEX idx_lms_projects_pipeline_id            ON lms_projects (pipeline_id);
CREATE INDEX idx_lms_levels_project_id               ON lms_levels (project_id);
CREATE INDEX idx_lms_chapters_level_id               ON lms_chapters (level_id);
CREATE INDEX idx_lms_chapters_trainer_id             ON lms_chapters (trainer_id);
CREATE INDEX idx_lms_ctr_trainer_id                  ON lms_chapter_trainer_requests (trainer_id);
CREATE INDEX idx_lms_ctr_reviewed_by                 ON lms_chapter_trainer_requests (reviewed_by);

-- LMS content

CREATE INDEX idx_lms_materials_chapter_id            ON lms_materials (chapter_id);
CREATE INDEX idx_lms_videos_chapter_id               ON lms_videos (chapter_id);
CREATE INDEX idx_lms_quizzes_chapter_id              ON lms_quizzes (chapter_id);
CREATE INDEX idx_lms_quiz_questions_quiz_id          ON lms_quiz_questions (quiz_id);
CREATE INDEX idx_lms_quiz_options_question_id        ON lms_quiz_options (question_id);

-- LMS categories, prompts & use cases

CREATE INDEX idx_lms_prompts_level_id                ON lms_prompts (level_id);
CREATE INDEX idx_lms_prompt_categories_category_id   ON lms_prompt_categories (category_id);
CREATE INDEX idx_lms_use_cases_level_id              ON lms_use_cases (level_id);
CREATE INDEX idx_lms_use_case_categories_category_id ON lms_use_case_categories (category_id);

-- LMS users & progress

CREATE INDEX idx_lms_tokens_user_id                  ON lms_tokens (user_id);
CREATE INDEX idx_lms_groups_project_id               ON lms_groups (project_id);
CREATE INDEX idx_lms_accesses_user_id                ON lms_accesses (user_id);
CREATE INDEX idx_lms_accesses_group_id               ON lms_accesses (group_id, project_id);
CREATE INDEX idx_lms_accesses_current_level_id       ON lms_accesses (current_level_id, project_id);
CREATE INDEX idx_lms_level_history_level_id          ON lms_level_history (level_id);
CREATE INDEX idx_lms_coaching_notes_student          ON lms_coaching_notes (student_access_id);
CREATE INDEX idx_lms_coaching_notes_champion         ON lms_coaching_notes (champion_access_id);
CREATE INDEX idx_lms_material_completions_material   ON lms_material_completions (material_id);
CREATE INDEX idx_lms_video_completions_video_id      ON lms_video_completions (video_id);
CREATE INDEX idx_lms_quiz_submissions_quiz_id        ON lms_quiz_submissions (quiz_id);
CREATE INDEX idx_lms_prompt_subs_prompt_id           ON lms_prompt_submissions (prompt_id);
CREATE INDEX idx_lms_prompt_subs_assigned_by         ON lms_prompt_submissions (assigned_by_access_id);
CREATE INDEX idx_lms_prompt_subs_reviewed_by         ON lms_prompt_submissions (reviewed_by_access_id);
CREATE INDEX idx_lms_use_case_subs_use_case_id       ON lms_use_case_submissions (use_case_id);
CREATE INDEX idx_lms_use_case_subs_assigned_by       ON lms_use_case_submissions (assigned_by_access_id);
CREATE INDEX idx_lms_use_case_subs_reviewed_by       ON lms_use_case_submissions (reviewed_by_access_id);

---------------
-- Functions --
---------------

CREATE OR REPLACE FUNCTION update_updated_at()
  RETURNS TRIGGER AS $$
  BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

--------------
-- Triggers --
--------------

-- LMS program structure

CREATE TRIGGER update_lms_projects_updated_at_trigger
  BEFORE UPDATE ON lms_projects
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_levels_updated_at_trigger
  BEFORE UPDATE ON lms_levels
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_chapters_updated_at_trigger
  BEFORE UPDATE ON lms_chapters
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_chapter_trainer_requests_updated_at_trigger
  BEFORE UPDATE ON lms_chapter_trainer_requests
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS content

CREATE TRIGGER update_lms_materials_updated_at_trigger
  BEFORE UPDATE ON lms_materials
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_videos_updated_at_trigger
  BEFORE UPDATE ON lms_videos
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_quizzes_updated_at_trigger
  BEFORE UPDATE ON lms_quizzes
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_quiz_questions_updated_at_trigger
  BEFORE UPDATE ON lms_quiz_questions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS categories, prompts & use cases

CREATE TRIGGER update_lms_prompts_updated_at_trigger
  BEFORE UPDATE ON lms_prompts
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_use_cases_updated_at_trigger
  BEFORE UPDATE ON lms_use_cases
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS users & progress

CREATE TRIGGER update_lms_users_updated_at_trigger
  BEFORE UPDATE ON lms_users
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_groups_updated_at_trigger
  BEFORE UPDATE ON lms_groups
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_accesses_updated_at_trigger
  BEFORE UPDATE ON lms_accesses
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_prompt_submissions_updated_at_trigger
  BEFORE UPDATE ON lms_prompt_submissions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_use_case_submissions_updated_at_trigger
  BEFORE UPDATE ON lms_use_case_submissions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS pre-assessment

CREATE TRIGGER update_lms_pre_assessment_reports_updated_at_trigger
  BEFORE UPDATE ON lms_pre_assessment_reports
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS misc

CREATE TRIGGER update_lms_announcement_updated_at_trigger
  BEFORE UPDATE ON lms_announcement
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
