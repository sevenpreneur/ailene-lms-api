-- Dynamic CFTE AIPF pre-assessment: item bank, scoring config and per-item answers, alongside the legacy lms_pre_assessments

-- 1. Enumerations

CREATE TYPE lms_pa_stage_enum AS ENUM (
  'pre',
  'post'
);

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
  'in_progress',
  'grading',
  'scored',
  'failed'
);

CREATE TYPE lms_job_level_enum AS ENUM (
  'staff',
  'manager',
  'head',
  'executive'
);

-- 2. Taxonomy

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

-- 3. Item bank

CREATE TABLE lms_pa_forms (
  id                SERIAL             PRIMARY KEY,
  code              VARCHAR            NOT NULL,
  version           SMALLINT           NOT NULL  DEFAULT 1,
  stage             lms_pa_stage_enum  NOT NULL,
  name              VARCHAR            NOT NULL,
  duration_minutes  SMALLINT           NOT NULL,
  status            status_enum        NOT NULL  DEFAULT 'inactive',
  published_at      TIMESTAMPTZ            NULL,
  created_at        TIMESTAMPTZ        NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMPTZ        NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (code, version),
  UNIQUE (id, stage)
);

CREATE TABLE lms_pa_items (
  id                 SERIAL                 PRIMARY KEY,
  form_id            INTEGER                NOT NULL,
  item_code          VARCHAR(10)            NOT NULL,
  section            lms_pa_section_enum    NOT NULL,
  type               lms_pa_item_type_enum  NOT NULL,
  domain_id          SMALLINT                   NULL,
  dimension          lms_pa_dimension_enum      NULL,
  cfte_level         SMALLINT                   NULL  CHECK (cfte_level BETWEEN 1 AND 3),
  question           TEXT                   NOT NULL,
  is_critical       BOOLEAN                NOT NULL  DEFAULT FALSE,
  is_behaviour_gate  BOOLEAN                NOT NULL  DEFAULT FALSE,
  overclaim_item_id  INTEGER                    NULL,
  grading_config     JSONB                      NULL,
  order_index        SMALLINT               NOT NULL  DEFAULT 0,
  created_at         TIMESTAMPTZ            NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ            NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (form_id, item_code),
  UNIQUE (id, form_id),
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

-- 4. Scoring config

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

CREATE TABLE lms_pa_targets (
  id           SERIAL                PRIMARY KEY,
  config_id    INTEGER               NOT NULL,
  role         lms_access_role_enum      NULL,
  job_level    lms_job_level_enum        NULL,
  target_band  SMALLINT              NOT NULL  CHECK (target_band BETWEEN 0 AND 5),
  UNIQUE (config_id, role),
  UNIQUE (config_id, job_level),
  CHECK (num_nonnulls(role, job_level) = 1)
);

CREATE TABLE lms_pa_target_clusters (
  target_id   INTEGER       NOT NULL,
  cluster_id  SMALLINT      NOT NULL,
  min_score   NUMERIC(5,2)  NOT NULL,
  PRIMARY KEY (target_id, cluster_id)
);

-- 5. Attempts & answers

CREATE TABLE lms_pa_attempts (
  id                     SERIAL                      PRIMARY KEY,
  access_id              CHAR(21)                    NOT NULL,
  form_id                INTEGER                     NOT NULL,
  stage                  lms_pa_stage_enum           NOT NULL,
  config_id              INTEGER                         NULL,
  status                 lms_pa_attempt_status_enum  NOT NULL  DEFAULT 'in_progress',
  p1                     NUMERIC(5,2)                    NULL,
  p2                     NUMERIC(5,2)                    NULL,
  p3                     NUMERIC(5,2)                    NULL,
  knowledge_score        NUMERIC(5,2)                    NULL,
  skills_score           NUMERIC(5,2)                    NULL,
  behaviours_score       NUMERIC(5,2)                    NULL,
  raw_band               SMALLINT                        NULL  CHECK (raw_band BETWEEN 0 AND 5),
  final_band             SMALLINT                        NULL  CHECK (final_band BETWEEN 0 AND 5),
  level                  SMALLINT                        NULL  CHECK (level BETWEEN 0 AND 3),
  target_band            SMALLINT                        NULL  CHECK (target_band BETWEEN 0 AND 5),
  security_gate_passed   BOOLEAN                         NULL,
  behaviour_gate_passed  BOOLEAN                         NULL,
  is_overclaim           BOOLEAN                         NULL,
  is_shadow_ai           BOOLEAN                         NULL,
  error_message          TEXT                            NULL,
  started_at             TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  submitted_at           TIMESTAMPTZ                     NULL,
  scored_at              TIMESTAMPTZ                     NULL,
  updated_at             TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (access_id, stage),
  UNIQUE (id, form_id)
);

CREATE TABLE lms_pa_answers (
  attempt_id          INTEGER       NOT NULL,
  item_id             INTEGER       NOT NULL,
  form_id             INTEGER       NOT NULL,
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

CREATE TABLE lms_pa_attempt_reports (
  id               SERIAL       PRIMARY KEY,
  attempt_id       INTEGER      NOT NULL  UNIQUE,
  status           VARCHAR      NOT NULL  DEFAULT 'pending',
  recommendations  JSONB            NULL,
  error_message    TEXT             NULL,
  queued_at        TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  generated_at     TIMESTAMPTZ      NULL,
  updated_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

-- 6. Job level on lms_accesses

ALTER TABLE lms_accesses
  ADD COLUMN job_level lms_job_level_enum NULL;

-- 7. References

ALTER TABLE lms_pa_domains
  ADD FOREIGN KEY (cluster_id) REFERENCES lms_pa_clusters (id);

ALTER TABLE lms_pa_items
  ADD FOREIGN KEY (form_id)                    REFERENCES lms_pa_forms (id),
  ADD FOREIGN KEY (domain_id)                  REFERENCES lms_pa_domains (id),
  ADD FOREIGN KEY (overclaim_item_id, form_id) REFERENCES lms_pa_items (id, form_id);

ALTER TABLE lms_pa_item_options
  ADD FOREIGN KEY (item_id) REFERENCES lms_pa_items (id);

ALTER TABLE lms_pa_bands
  ADD FOREIGN KEY (config_id) REFERENCES lms_pa_scoring_configs (id);

ALTER TABLE lms_pa_targets
  ADD FOREIGN KEY (config_id) REFERENCES lms_pa_scoring_configs (id);

ALTER TABLE lms_pa_target_clusters
  ADD FOREIGN KEY (target_id)  REFERENCES lms_pa_targets (id),
  ADD FOREIGN KEY (cluster_id) REFERENCES lms_pa_clusters (id);

ALTER TABLE lms_pa_attempts
  ADD FOREIGN KEY (access_id)       REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (form_id, stage)  REFERENCES lms_pa_forms (id, stage),
  ADD FOREIGN KEY (config_id)       REFERENCES lms_pa_scoring_configs (id);

ALTER TABLE lms_pa_answers
  ADD FOREIGN KEY (attempt_id, form_id) REFERENCES lms_pa_attempts (id, form_id),
  ADD FOREIGN KEY (item_id, form_id)    REFERENCES lms_pa_items (id, form_id),
  ADD FOREIGN KEY (option_id, item_id)  REFERENCES lms_pa_item_options (id, item_id);

ALTER TABLE lms_pa_attempt_domain_scores
  ADD FOREIGN KEY (attempt_id) REFERENCES lms_pa_attempts (id),
  ADD FOREIGN KEY (domain_id)  REFERENCES lms_pa_domains (id);

ALTER TABLE lms_pa_attempt_reports
  ADD FOREIGN KEY (attempt_id) REFERENCES lms_pa_attempts (id);

-- 8. Indexes

CREATE UNIQUE INDEX idx_lms_pa_forms_active_stage    ON lms_pa_forms (stage) WHERE status = 'active';
CREATE UNIQUE INDEX idx_lms_pa_scoring_configs_active ON lms_pa_scoring_configs (is_active) WHERE is_active;
CREATE INDEX idx_lms_pa_domains_cluster_id           ON lms_pa_domains (cluster_id);
CREATE INDEX idx_lms_pa_items_domain_id              ON lms_pa_items (domain_id);
CREATE INDEX idx_lms_pa_target_clusters_cluster_id   ON lms_pa_target_clusters (cluster_id);
CREATE INDEX idx_lms_pa_attempts_form_id             ON lms_pa_attempts (form_id);
CREATE INDEX idx_lms_pa_attempts_config_id           ON lms_pa_attempts (config_id);
CREATE INDEX idx_lms_pa_answers_item_id              ON lms_pa_answers (item_id);
CREATE INDEX idx_lms_pa_answers_option_id            ON lms_pa_answers (option_id);
CREATE INDEX idx_lms_pa_attempt_domain_scores_domain ON lms_pa_attempt_domain_scores (domain_id);

-- 9. Triggers

CREATE TRIGGER update_lms_pa_forms_updated_at_trigger
  BEFORE UPDATE ON lms_pa_forms
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

CREATE TRIGGER update_lms_pa_attempt_reports_updated_at_trigger
  BEFORE UPDATE ON lms_pa_attempt_reports
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 10. Seed: clusters, domains and the default scoring config from the proposal

INSERT INTO lms_pa_clusters (code, name, order_index) VALUES
  ('understand', 'Memahami AI',                1),
  ('use',        'Memakai AI',                 2),
  ('safe',       'Aman dan bertanggung jawab', 3),
  ('lead',       'Memimpin adopsi',            4);

INSERT INTO lms_pa_domains (cluster_id, code, cfte_number, name) VALUES
  ((SELECT id FROM lms_pa_clusters WHERE code = 'understand'), 'foundations',    1,  'AI Foundations'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'use'),        'applications',   2,  'AI Applications and Use Cases'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'use'),        'tools_methods',  3,  'AI Tools and Methods'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'use'),        'data',           4,  'Data'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'safe'),       'risks',          5,  'AI Risks and Limitations'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'safe'),       'ethics',         6,  'Regulation, Ethics, and Accountability'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'lead'),       'implementation', 7,  'AI Implementation and Operationalisation'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'lead'),       'strategy',       8,  'AI Strategy and Business Impact'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'understand'), 'emerging',       9,  'Emerging Trends and Industry Evolution'),
  ((SELECT id FROM lms_pa_clusters WHERE code = 'understand'), 'tech_landscape', 10, 'Technology Landscape');

INSERT INTO lms_pa_scoring_configs (name, is_active) VALUES ('Default v1 (pre-calibration)', TRUE);

INSERT INTO lms_pa_bands (config_id, band, name, level, min_p1, min_p2, min_p3) VALUES
  (1, 0, 'Unmeasured baseline',  0, NULL, NULL, NULL),
  (1, 1, 'Awareness',            0, 40,   NULL, NULL),
  (1, 2, 'Basic Proficiency',    1, 70,   NULL, NULL),
  (1, 3, 'Working Proficiency',  2, 70,   60,   NULL),
  (1, 4, 'Advanced Application', 3, 70,   70,   50),
  (1, 5, 'Strategic Mastery',    3, 70,   80,   75);

INSERT INTO lms_pa_targets (config_id, role, job_level, target_band) VALUES
  (1, NULL,       'staff',     2),
  (1, NULL,       'manager',   3),
  (1, NULL,       'head',      3),
  (1, NULL,       'executive', 3),
  (1, 'champion', NULL,        4);

INSERT INTO lms_pa_target_clusters (target_id, cluster_id, min_score) VALUES
  ((SELECT id FROM lms_pa_targets WHERE job_level = 'head'),      (SELECT id FROM lms_pa_clusters WHERE code = 'lead'), 60),
  ((SELECT id FROM lms_pa_targets WHERE job_level = 'executive'), (SELECT id FROM lms_pa_clusters WHERE code = 'lead'), 70),
  ((SELECT id FROM lms_pa_targets WHERE job_level = 'executive'), (SELECT id FROM lms_pa_clusters WHERE code = 'safe'), 70);
