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

-- Enumeration for the lms_pa_* tables

CREATE TYPE lms_pa_section_enum AS ENUM (
  'profile',
  'scenario',
  'practice',
  'behaviour'
);

CREATE TYPE lms_pa_dimension_enum AS ENUM (
  'knowledge',
  'skills',
  'behaviours'
);

CREATE TYPE lms_pa_item_type_enum AS ENUM (
  'single_choice',
  'text'
);

CREATE TYPE lms_pa_attempt_status_enum AS ENUM (
  'submitted',
  'scored'
);

------------
-- Tables --
------------

-- Lookup

CREATE TABLE lms_levels (
  id            SERIAL       PRIMARY KEY,
  level_number  SMALLINT     NOT NULL  UNIQUE,
  name          VARCHAR      NOT NULL,
  status        status_enum  NOT NULL  DEFAULT 'active',
  created_at    TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_categories (
  id    SMALLSERIAL  PRIMARY KEY,
  name  VARCHAR      NOT NULL  UNIQUE
);

-- Project & access

CREATE TABLE lms_users (
  id              UUID         PRIMARY KEY,
  full_name       VARCHAR      NOT NULL,
  email           VARCHAR      NOT NULL  UNIQUE,
  avatar          VARCHAR          NULL,
  job_title       VARCHAR      NOT NULL,
  password_hash   VARCHAR(255)     NULL,
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

CREATE TABLE lms_projects (
  id                 CHAR(21)     PRIMARY KEY,
  name               VARCHAR      NOT NULL,
  attendee_pax       INTEGER          NULL,
  company_name       VARCHAR          NULL,
  company_image_url  VARCHAR          NULL,
  company_slug       VARCHAR          NULL,
  created_at         TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
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
  group_id           INTEGER               NOT NULL,
  current_level_id   INTEGER               NOT NULL  DEFAULT 1,
  role               lms_access_role_enum  NOT NULL,
  created_at         TIMESTAMPTZ           NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ           NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (project_id, user_id)
);

-- Contents

CREATE TABLE lms_chapters (
  id           SERIAL       PRIMARY KEY,
  level_id     INTEGER      NOT NULL,
  project_id   CHAR(21)     NOT NULL,
  name         VARCHAR      NOT NULL,
  description  TEXT             NULL,
  status       status_enum  NOT NULL  DEFAULT 'active',
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (id, project_id)
);

CREATE TABLE lms_chapter_sessions (
  id                SERIAL                   PRIMARY KEY,
  chapter_id        INTEGER                  NOT NULL,
  project_id        CHAR(21)                 NOT NULL,
  only_group_id     INTEGER                      NULL,
  session_date      TIMESTAMPTZ              NOT NULL,
  duration_minutes  INTEGER                  NOT NULL,
  location_name     VARCHAR                  NOT NULL,
  location_url      VARCHAR                  NOT NULL,
  method            lms_chapter_method_enum  NOT NULL  DEFAULT 'offline',
  trainer_id        CHAR(21)                     NULL,
  status            status_enum              NOT NULL  DEFAULT 'active',
  created_at        TIMESTAMPTZ              NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMPTZ              NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (chapter_id, only_group_id)
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

-- Practical Learning

CREATE TABLE lms_prompts (
  id               SERIAL       PRIMARY KEY,
  level_id         INTEGER      NOT NULL,
  only_project_id  CHAR(21)         NULL,
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
  only_project_id  CHAR(21)         NULL,
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

CREATE TABLE lms_assignment_drafts (
  id                  SERIAL        PRIMARY KEY,
  batch_id            CHAR(21)      NOT NULL,
  champion_access_id  CHAR(21)      NOT NULL,
  project_id          CHAR(21)      NOT NULL,
  instruction         TEXT          NOT NULL,
  requested_kind      VARCHAR(10)       NULL  CHECK (requested_kind IN ('PROMPT', 'USE_CASE')),
  kind                VARCHAR(10)   NOT NULL  CHECK (kind IN ('PROMPT', 'USE_CASE')),
  angle               VARCHAR(100)  NOT NULL,
  name                VARCHAR(255)  NOT NULL,
  description         TEXT          NOT NULL,
  expected_output     TEXT              NULL,
  category_ids        SMALLINT[]    NOT NULL,
  used_at             TIMESTAMPTZ       NULL,
  used_prompt_id      INTEGER           NULL,
  used_use_case_id    INTEGER           NULL,
  created_at          TIMESTAMPTZ   NOT NULL  DEFAULT CURRENT_TIMESTAMP
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
  ai_status                VARCHAR(10)      NULL  CHECK (ai_status IN ('pending', 'completed', 'failed')),
  ai_feedback              TEXT             NULL,
  ai_evaluated_at          TIMESTAMPTZ      NULL,
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

CREATE TABLE lms_pa_clusters (
  id           SMALLSERIAL  PRIMARY KEY,
  code         VARCHAR      NOT NULL  UNIQUE,
  name         VARCHAR      NOT NULL,
  order_index  SMALLINT     NOT NULL  DEFAULT 0
);

CREATE TABLE lms_pa_domains (
  id            SMALLSERIAL  PRIMARY KEY,
  cluster_id    SMALLINT     NOT NULL,
  code          VARCHAR      NOT NULL  UNIQUE,
  cfte_number   SMALLINT     NOT NULL  UNIQUE  CHECK (cfte_number BETWEEN 1 AND 10),
  name          VARCHAR      NOT NULL
);

CREATE TABLE lms_pa_versions (
  id                SERIAL       PRIMARY KEY,
  code              VARCHAR      NOT NULL,
  version           SMALLINT     NOT NULL  DEFAULT 1,
  name              VARCHAR      NOT NULL,
  duration_minutes  SMALLINT     NOT NULL,
  created_at        TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (code, version)
);

CREATE TABLE lms_pa_project_versions (
  project_id  CHAR(21)     PRIMARY KEY,
  version_id  INTEGER      NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_pa_items (
  id                 SERIAL                 PRIMARY KEY,
  version_id         INTEGER                NOT NULL,
  item_code          VARCHAR(10)            NOT NULL,
  section            lms_pa_section_enum    NOT NULL,
  type               lms_pa_item_type_enum  NOT NULL,
  domain_id          SMALLINT                   NULL,
  dimension          lms_pa_dimension_enum      NULL,
  cfte_level         SMALLINT                   NULL  CHECK (cfte_level BETWEEN 1 AND 3),
  question           TEXT                   NOT NULL,
  is_critical        BOOLEAN                NOT NULL  DEFAULT FALSE,
  is_behaviour_gate  BOOLEAN                NOT NULL  DEFAULT FALSE,
  overclaim_item_id  INTEGER                    NULL,
  grading_config     JSONB                      NULL,
  order_index        SMALLINT               NOT NULL  DEFAULT 0,
  created_at         TIMESTAMPTZ            NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ            NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (version_id, item_code),
  UNIQUE (id, version_id),
  CHECK (section = 'profile' OR (domain_id IS NOT NULL AND dimension IS NOT NULL)),
  CHECK (grading_config IS NULL OR type = 'text')
);

CREATE TABLE lms_pa_item_options (
  id            SERIAL        PRIMARY KEY,
  item_id       INTEGER       NOT NULL,
  option_code   VARCHAR(10)   NOT NULL,
  text          TEXT          NOT NULL,
  credit        NUMERIC(3,2)      NULL  CHECK (credit BETWEEN 0 AND 1),
  is_dont_know  BOOLEAN       NOT NULL  DEFAULT FALSE,
  flag          VARCHAR(30)       NULL,
  order_index   SMALLINT      NOT NULL  DEFAULT 0,
  UNIQUE (item_id, option_code),
  UNIQUE (id, item_id)
);

CREATE TABLE lms_pa_scoring_configs (
  id                         SERIAL        PRIMARY KEY,
  name                       VARCHAR       NOT NULL,
  is_active                  BOOLEAN       NOT NULL  DEFAULT FALSE,
  security_gate_max_band     SMALLINT      NOT NULL  DEFAULT 1,
  behaviour_gate_from_band   SMALLINT      NOT NULL  DEFAULT 3,
  behaviour_gate_max_band    SMALLINT      NOT NULL  DEFAULT 2,
  behaviour_gate_min_score   NUMERIC(5,2)  NOT NULL  DEFAULT 50,
  behaviour_gate_min_credit  NUMERIC(3,2)  NOT NULL  DEFAULT 0.50,
  overclaim_min_credit       NUMERIC(3,2)  NOT NULL  DEFAULT 0.75,
  overclaim_capped_credit    NUMERIC(3,2)  NOT NULL  DEFAULT 0.25,
  target_band                SMALLINT      NOT NULL  DEFAULT 2  CHECK (target_band BETWEEN 0 AND 5),
  created_at                 TIMESTAMPTZ   NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_pa_bands (
  config_id  INTEGER       NOT NULL,
  band       SMALLINT      NOT NULL  CHECK (band BETWEEN 0 AND 5),
  name       VARCHAR       NOT NULL,
  level      SMALLINT      NOT NULL  CHECK (level BETWEEN 0 AND 3),
  min_p1     NUMERIC(5,2)      NULL,
  min_p2     NUMERIC(5,2)      NULL,
  min_p3     NUMERIC(5,2)      NULL,
  PRIMARY KEY (config_id, band)
);

CREATE TABLE lms_pa_attempts (
  id                     SERIAL                      PRIMARY KEY,
  access_id              CHAR(21)                    NOT NULL,
  version_id             INTEGER                     NOT NULL,
  config_id              INTEGER                         NULL,
  status                 lms_pa_attempt_status_enum  NOT NULL  DEFAULT 'submitted',
  p1                     NUMERIC(5,2)                    NULL,
  p2                     NUMERIC(5,2)                    NULL,
  p3                     NUMERIC(5,2)                    NULL,
  knowledge_score        NUMERIC(5,2)                    NULL,
  skills_score           NUMERIC(5,2)                    NULL,
  behaviours_score       NUMERIC(5,2)                    NULL,
  raw_band               SMALLINT                        NULL  CHECK (raw_band BETWEEN 0 AND 5),
  final_band             SMALLINT                        NULL  CHECK (final_band BETWEEN 0 AND 5),
  level                  SMALLINT                        NULL  CHECK (level BETWEEN 0 AND 3),
  security_gate_passed   BOOLEAN                         NULL,
  behaviour_gate_passed  BOOLEAN                         NULL,
  is_overclaim           BOOLEAN                         NULL,
  is_shadow_ai           BOOLEAN                         NULL,
  started_at             TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  submitted_at           TIMESTAMPTZ                     NULL,
  scored_at              TIMESTAMPTZ                     NULL,
  updated_at             TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (access_id),
  UNIQUE (id, version_id)
);

CREATE TABLE lms_pa_answers (
  attempt_id          INTEGER       NOT NULL,
  item_id             INTEGER       NOT NULL,
  version_id          INTEGER       NOT NULL,
  option_id           INTEGER           NULL,
  text_value          TEXT              NULL,
  credit              NUMERIC(3,2)      NULL  CHECK (credit BETWEEN 0 AND 1),
  rubric_scores       JSONB             NULL,
  rubric_total        SMALLINT          NULL,
  has_sensitive_data  BOOLEAN       NOT NULL  DEFAULT FALSE,
  ai_feedback         TEXT              NULL,
  graded_at           TIMESTAMPTZ       NULL,
  answered_at         TIMESTAMPTZ   NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMPTZ   NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (attempt_id, item_id),
  CHECK (num_nonnulls(option_id, text_value) = 1)
);

CREATE TABLE lms_pa_attempt_domain_scores (
  attempt_id  INTEGER       NOT NULL,
  domain_id   SMALLINT      NOT NULL,
  score       NUMERIC(5,2)  NOT NULL,
  item_count  SMALLINT      NOT NULL,
  PRIMARY KEY (attempt_id, domain_id)
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

ALTER TABLE lms_chapters
  ADD FOREIGN KEY (level_id)   REFERENCES lms_levels (id),
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_chapter_sessions
  ADD FOREIGN KEY (chapter_id, project_id)    REFERENCES lms_chapters (id, project_id),
  ADD FOREIGN KEY (only_group_id, project_id) REFERENCES lms_groups (id, project_id),
  ADD FOREIGN KEY (trainer_id)                REFERENCES trainers (id);

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
  ADD FOREIGN KEY (level_id)        REFERENCES lms_levels (id),
  ADD FOREIGN KEY (only_project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_prompt_categories
  ADD FOREIGN KEY (prompt_id)   REFERENCES lms_prompts (id),
  ADD FOREIGN KEY (category_id) REFERENCES lms_categories (id);

ALTER TABLE lms_use_cases
  ADD FOREIGN KEY (level_id)        REFERENCES lms_levels (id),
  ADD FOREIGN KEY (only_project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_use_case_categories
  ADD FOREIGN KEY (use_case_id) REFERENCES lms_use_cases (id),
  ADD FOREIGN KEY (category_id) REFERENCES lms_categories (id);

ALTER TABLE lms_assignment_drafts
  ADD FOREIGN KEY (champion_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (project_id)         REFERENCES lms_projects (id),
  ADD FOREIGN KEY (used_prompt_id)     REFERENCES lms_prompts (id),
  ADD FOREIGN KEY (used_use_case_id)   REFERENCES lms_use_cases (id);

-- LMS users & progress

ALTER TABLE lms_tokens
  ADD FOREIGN KEY (user_id) REFERENCES lms_users (id);

ALTER TABLE lms_groups
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_accesses
  ADD FOREIGN KEY (project_id)                   REFERENCES lms_projects (id),
  ADD FOREIGN KEY (user_id)                      REFERENCES lms_users (id),
  ADD FOREIGN KEY (group_id, project_id)         REFERENCES lms_groups (id, project_id),
  ADD FOREIGN KEY (current_level_id)             REFERENCES lms_levels (id);

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

ALTER TABLE lms_pa_domains
  ADD FOREIGN KEY (cluster_id) REFERENCES lms_pa_clusters (id);

ALTER TABLE lms_pa_project_versions
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id),
  ADD FOREIGN KEY (version_id) REFERENCES lms_pa_versions (id);

ALTER TABLE lms_pa_items
  ADD FOREIGN KEY (version_id)                    REFERENCES lms_pa_versions (id),
  ADD FOREIGN KEY (domain_id)                     REFERENCES lms_pa_domains (id),
  ADD FOREIGN KEY (overclaim_item_id, version_id) REFERENCES lms_pa_items (id, version_id);

ALTER TABLE lms_pa_item_options
  ADD FOREIGN KEY (item_id) REFERENCES lms_pa_items (id);

ALTER TABLE lms_pa_bands
  ADD FOREIGN KEY (config_id) REFERENCES lms_pa_scoring_configs (id);

ALTER TABLE lms_pa_attempts
  ADD FOREIGN KEY (access_id)  REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (version_id) REFERENCES lms_pa_versions (id),
  ADD FOREIGN KEY (config_id)  REFERENCES lms_pa_scoring_configs (id);

ALTER TABLE lms_pa_answers
  ADD FOREIGN KEY (attempt_id, version_id) REFERENCES lms_pa_attempts (id, version_id),
  ADD FOREIGN KEY (item_id, version_id)    REFERENCES lms_pa_items (id, version_id),
  ADD FOREIGN KEY (option_id, item_id)     REFERENCES lms_pa_item_options (id, item_id);

ALTER TABLE lms_pa_attempt_domain_scores
  ADD FOREIGN KEY (attempt_id) REFERENCES lms_pa_attempts (id),
  ADD FOREIGN KEY (domain_id)  REFERENCES lms_pa_domains (id);

-- LMS misc

ALTER TABLE lms_announcement
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id);

-------------
-- Indexes --
-------------

-- LMS program structure

CREATE INDEX idx_lms_chapters_level_id               ON lms_chapters (level_id);
CREATE INDEX idx_lms_chapters_project_id             ON lms_chapters (project_id);
CREATE INDEX idx_lms_chapter_sessions_chapter_id     ON lms_chapter_sessions (chapter_id);
CREATE INDEX idx_lms_chapter_sessions_only_group_id  ON lms_chapter_sessions (only_group_id);
CREATE INDEX idx_lms_chapter_sessions_trainer_id     ON lms_chapter_sessions (trainer_id);

-- UNIQUE (chapter_id, only_group_id) does not constrain repeated NULLs, so cap the all-groups session here.
CREATE UNIQUE INDEX idx_lms_chapter_sessions_all_groups
  ON lms_chapter_sessions (chapter_id) WHERE only_group_id IS NULL;

-- LMS content

CREATE INDEX idx_lms_materials_chapter_id            ON lms_materials (chapter_id);
CREATE INDEX idx_lms_videos_chapter_id               ON lms_videos (chapter_id);
CREATE INDEX idx_lms_quizzes_chapter_id              ON lms_quizzes (chapter_id);
CREATE INDEX idx_lms_quiz_questions_quiz_id          ON lms_quiz_questions (quiz_id);
CREATE INDEX idx_lms_quiz_options_question_id        ON lms_quiz_options (question_id);

-- LMS categories, prompts & use cases

CREATE INDEX idx_lms_prompts_only_project_id         ON lms_prompts (only_project_id);
CREATE INDEX idx_lms_use_cases_only_project_id       ON lms_use_cases (only_project_id);
CREATE INDEX idx_lms_prompts_level_id                ON lms_prompts (level_id);
CREATE INDEX idx_lms_prompt_categories_category_id   ON lms_prompt_categories (category_id);
CREATE INDEX idx_lms_use_cases_level_id              ON lms_use_cases (level_id);
CREATE INDEX idx_lms_use_case_categories_category_id ON lms_use_case_categories (category_id);
CREATE INDEX idx_lms_assignment_drafts_champion     ON lms_assignment_drafts (champion_access_id, created_at);

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

-- LMS pre-assessment

CREATE UNIQUE INDEX idx_lms_pa_scoring_configs_active ON lms_pa_scoring_configs (is_active) WHERE is_active;
CREATE INDEX idx_lms_pa_project_versions_version_id ON lms_pa_project_versions (version_id);
CREATE INDEX idx_lms_pa_domains_cluster_id           ON lms_pa_domains (cluster_id);
CREATE INDEX idx_lms_pa_items_domain_id              ON lms_pa_items (domain_id);
CREATE INDEX idx_lms_pa_attempts_version_id          ON lms_pa_attempts (version_id);
CREATE INDEX idx_lms_pa_attempts_config_id           ON lms_pa_attempts (config_id);
CREATE INDEX idx_lms_pa_answers_item_id              ON lms_pa_answers (item_id);
CREATE INDEX idx_lms_pa_answers_option_id            ON lms_pa_answers (option_id);
CREATE INDEX idx_lms_pa_attempt_domain_scores_domain ON lms_pa_attempt_domain_scores (domain_id);

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

CREATE TRIGGER update_lms_chapter_sessions_updated_at_trigger
  BEFORE UPDATE ON lms_chapter_sessions
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

CREATE TRIGGER update_lms_pa_versions_updated_at_trigger
  BEFORE UPDATE ON lms_pa_versions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_pa_project_versions_updated_at_trigger
  BEFORE UPDATE ON lms_pa_project_versions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_pa_items_updated_at_trigger
  BEFORE UPDATE ON lms_pa_items
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_pa_attempts_updated_at_trigger
  BEFORE UPDATE ON lms_pa_attempts
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_pa_answers_updated_at_trigger
  BEFORE UPDATE ON lms_pa_answers
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- LMS misc

CREATE TRIGGER update_lms_announcement_updated_at_trigger
  BEFORE UPDATE ON lms_announcement
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
