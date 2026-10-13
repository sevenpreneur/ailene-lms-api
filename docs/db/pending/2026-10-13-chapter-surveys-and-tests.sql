-- Apply once to Neon (project Ailene) before deploying the API that maps these tables; ddl-auto: validate fails without them.
-- Already folded into docs/db/ailene-lms.sql. Delete this file once it has been applied.

BEGIN;

CREATE TYPE lms_chapter_test_kind_enum AS ENUM (
  'pre_test',
  'post_test'
);

CREATE TYPE lms_chapter_test_question_type_enum AS ENUM (
  'multiple_choice',
  'essay'
);

CREATE TABLE lms_chapter_tests (
  id           SERIAL                      PRIMARY KEY,
  chapter_id   INTEGER                     NOT NULL,
  kind         lms_chapter_test_kind_enum  NOT NULL,
  name         VARCHAR                     NOT NULL,
  description  TEXT                            NULL,
  status       status_enum                 NOT NULL  DEFAULT 'active',
  created_at   TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ                 NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (chapter_id, kind)
);

CREATE TABLE lms_chapter_test_questions (
  id           SERIAL                               PRIMARY KEY,
  test_id      INTEGER                              NOT NULL,
  type         lms_chapter_test_question_type_enum  NOT NULL,
  question     TEXT                                 NOT NULL,
  explanation  TEXT                                     NULL,
  order_index  SMALLINT                             NOT NULL  DEFAULT 0,
  created_at   TIMESTAMPTZ                          NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ                          NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lms_chapter_test_options (
  id           SERIAL   PRIMARY KEY,
  question_id  INTEGER  NOT NULL,
  option_code  VARCHAR  NOT NULL,
  text         VARCHAR  NOT NULL,
  is_correct   BOOLEAN  NOT NULL  DEFAULT FALSE,
  UNIQUE (question_id, option_code)
);

CREATE TABLE lms_chapter_test_submissions (
  id                 SERIAL       PRIMARY KEY,
  student_access_id  CHAR(21)     NOT NULL,
  test_id            INTEGER      NOT NULL,
  answers            JSONB        NOT NULL,
  score              SMALLINT         NULL  CHECK (score BETWEEN 0 AND 100),
  correct_count      SMALLINT     NOT NULL,
  choice_count       SMALLINT     NOT NULL,
  submitted_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, test_id)
);

CREATE TABLE lms_chapter_surveys (
  id                 SERIAL       PRIMARY KEY,
  student_access_id  CHAR(21)     NOT NULL,
  chapter_id         INTEGER      NOT NULL,
  session_id         INTEGER      NOT NULL,
  satisfaction       SMALLINT     NOT NULL  CHECK (satisfaction BETWEEN 1 AND 5),
  relevance          SMALLINT     NOT NULL  CHECK (relevance BETWEEN 1 AND 5),
  trainer_rating     SMALLINT     NOT NULL  CHECK (trainer_rating BETWEEN 1 AND 5),
  comment            TEXT             NULL,
  submitted_at       TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_access_id, chapter_id)
);

ALTER TABLE lms_chapter_tests
  ADD FOREIGN KEY (chapter_id) REFERENCES lms_chapters (id);

ALTER TABLE lms_chapter_test_questions
  ADD FOREIGN KEY (test_id) REFERENCES lms_chapter_tests (id);

ALTER TABLE lms_chapter_test_options
  ADD FOREIGN KEY (question_id) REFERENCES lms_chapter_test_questions (id);

ALTER TABLE lms_chapter_test_submissions
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (test_id)           REFERENCES lms_chapter_tests (id);

ALTER TABLE lms_chapter_surveys
  ADD FOREIGN KEY (student_access_id) REFERENCES lms_accesses (id),
  ADD FOREIGN KEY (chapter_id)        REFERENCES lms_chapters (id),
  ADD FOREIGN KEY (session_id)        REFERENCES lms_chapter_sessions (id);

CREATE INDEX idx_lms_chapter_test_questions_test_id  ON lms_chapter_test_questions (test_id);
CREATE INDEX idx_lms_chapter_test_options_question   ON lms_chapter_test_options (question_id);
CREATE INDEX idx_lms_chapter_test_subs_test_id       ON lms_chapter_test_submissions (test_id);
CREATE INDEX idx_lms_chapter_surveys_chapter_id      ON lms_chapter_surveys (chapter_id);
CREATE INDEX idx_lms_chapter_surveys_session_id      ON lms_chapter_surveys (session_id);

CREATE TRIGGER update_lms_chapter_tests_updated_at_trigger
  BEFORE UPDATE ON lms_chapter_tests
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_lms_chapter_test_questions_updated_at_trigger
  BEFORE UPDATE ON lms_chapter_test_questions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

COMMIT;

-- Optional example content for one chapter (replace 5 with a real lms_chapters.id), uncomment to use.
--
-- WITH pre AS (
--   INSERT INTO lms_chapter_tests (chapter_id, kind, name, description)
--   VALUES (5, 'pre_test', 'Pre-test — Fondasi AI', 'Ukur pemahaman awal sebelum belajar chapter ini.')
--   RETURNING id
-- ), q1 AS (
--   INSERT INTO lms_chapter_test_questions (test_id, type, question, explanation, order_index)
--   SELECT id, 'multiple_choice', 'Apa yang paling tepat menggambarkan cara kerja large language model?',
--          'LLM memprediksi token berikutnya berdasarkan pola dari data latihnya.', 1 FROM pre
--   RETURNING id
-- ), q1_options AS (
--   INSERT INTO lms_chapter_test_options (question_id, option_code, text, is_correct)
--   SELECT q1.id, o.code, o.text, o.correct FROM q1,
--     (VALUES ('A', 'Mencari jawaban dari basis data fakta', false),
--             ('B', 'Memprediksi kata berikutnya dari pola data latih', true),
--             ('C', 'Menjalankan aturan if-else yang ditulis manusia', false)) AS o(code, text, correct)
-- )
-- INSERT INTO lms_chapter_test_questions (test_id, type, question, order_index)
-- SELECT id, 'essay', 'Ceritakan satu pekerjaanmu yang menurutmu bisa dibantu AI, dan kenapa.', 2 FROM pre;
--
-- Repeat with kind = 'post_test' (ideally the same questions) to measure the gain.
