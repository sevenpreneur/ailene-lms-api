CREATE TABLE IF NOT EXISTS lms_guided_use_case_sessions (
  id                         SERIAL PRIMARY KEY,
  student_access_id          CHAR(21) NOT NULL REFERENCES lms_accesses(id) ON DELETE CASCADE,
  project_id                 CHAR(21) NOT NULL REFERENCES lms_projects(id) ON DELETE CASCADE,
  use_case_id                INTEGER REFERENCES lms_use_cases(id) ON DELETE CASCADE,
  status                     VARCHAR(24) NOT NULL CHECK (status IN ('intake','steps_ready','in_progress','ready_to_submit','submitted')),
  current_step               INTEGER NOT NULL CHECK (current_step BETWEEN 1 AND 6),
  daily_task                 TEXT,
  frequency                  lms_use_case_frequency_enum,
  example_text               TEXT,
  example_file_url           VARCHAR(500),
  sensitive_data_confirmed   BOOLEAN NOT NULL,
  minutes_without_ai         NUMERIC(8,2) CHECK (minutes_without_ai > 0 AND minutes_without_ai <= 599999.40),
  current_process            TEXT,
  current_tools              JSONB NOT NULL CHECK (jsonb_typeof(current_tools) = 'array'),
  pain_point                 TEXT,
  steps                      JSONB NOT NULL CHECK (jsonb_typeof(steps) = 'array'),
  summary                    TEXT,
  estimated_minutes_with_ai  INTEGER CHECK (estimated_minutes_with_ai > 0),
  risks                      JSONB NOT NULL CHECK (jsonb_typeof(risks) = 'array'),
  generations_used           INTEGER NOT NULL CHECK (generations_used BETWEEN 0 AND 3),
  generation_token           CHAR(21),
  generation_started_at      TIMESTAMPTZ,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK ((generation_token IS NULL) = (generation_started_at IS NULL)),
  CHECK (status <> 'submitted' OR use_case_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_lms_guided_use_case_assignment
  ON lms_guided_use_case_sessions(student_access_id, use_case_id) WHERE use_case_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_lms_guided_use_case_active_draft
  ON lms_guided_use_case_sessions(student_access_id, project_id)
  WHERE use_case_id IS NULL AND status <> 'submitted';
CREATE INDEX IF NOT EXISTS idx_lms_guided_use_case_project
  ON lms_guided_use_case_sessions(project_id);

CREATE TABLE IF NOT EXISTS lms_guided_use_case_generations (
  id          SERIAL PRIMARY KEY,
  session_id  INTEGER NOT NULL REFERENCES lms_guided_use_case_sessions(id) ON DELETE CASCADE,
  version     INTEGER NOT NULL,
  feedback    TEXT,
  workflow    JSONB NOT NULL CHECK (jsonb_typeof(workflow) = 'object'),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (session_id, version)
);

ALTER TABLE lms_use_case_submissions
  ADD COLUMN IF NOT EXISTS guided_session_id INTEGER UNIQUE REFERENCES lms_guided_use_case_sessions(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS testimonial TEXT,
  ADD COLUMN IF NOT EXISTS helpfulness_rating INTEGER CHECK (helpfulness_rating BETWEEN 1 AND 5),
  ADD COLUMN IF NOT EXISTS share_consent BOOLEAN,
  ADD COLUMN IF NOT EXISTS hours_saved NUMERIC(6,2) CHECK (hours_saved >= 0);
