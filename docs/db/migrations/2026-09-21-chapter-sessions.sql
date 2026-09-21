-- Split lms_chapters into content + per-group sessions, drop lms_chapter_trainer_requests. Guarded, re-runnable.

BEGIN;

-- 1. Sessions carry the schedule. only_group_id NULL means every group in the project attends.

CREATE TABLE IF NOT EXISTS lms_chapter_sessions (
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

-- An earlier run of this script created the column as group_id; RENAME has no IF EXISTS.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_name = 'lms_chapter_sessions' AND column_name = 'group_id') THEN
    ALTER TABLE lms_chapter_sessions RENAME COLUMN group_id TO only_group_id;
  END IF;
END $$;

-- 2. project_id on chapters lets the composite FKs below pin level and group to the same project.

ALTER TABLE lms_chapters ADD COLUMN IF NOT EXISTS project_id CHAR(21) NULL;

UPDATE lms_chapters c
   SET project_id = lv.project_id
  FROM lms_levels lv
 WHERE lv.id = c.level_id AND c.project_id IS NULL;

ALTER TABLE lms_chapters ALTER COLUMN project_id SET NOT NULL;

ALTER TABLE lms_chapters DROP CONSTRAINT IF EXISTS lms_chapters_id_project_id_key;
ALTER TABLE lms_chapters ADD CONSTRAINT lms_chapters_id_project_id_key UNIQUE (id, project_id);

-- 3. Every existing chapter becomes one all-groups session, keeping its schedule.

INSERT INTO lms_chapter_sessions (chapter_id, project_id, only_group_id, session_date, duration_minutes,
                                  location_name, location_url, method, trainer_id, status,
                                  created_at, updated_at)
SELECT c.id, c.project_id, NULL, c.session_date, c.duration_minutes,
       c.location_name, c.location_url, c.method, c.trainer_id, c.status,
       c.created_at, c.updated_at
  FROM lms_chapters c
 WHERE NOT EXISTS (SELECT 1 FROM lms_chapter_sessions s
                    WHERE s.chapter_id = c.id AND s.only_group_id IS NULL);

-- 4. The trainer pipeline is out of scope for now.

DROP TABLE IF EXISTS lms_chapter_trainer_requests;
DROP TYPE IF EXISTS lms_chapter_trainer_request_status_enum;

-- 5. The schedule now lives only on sessions.

ALTER TABLE lms_chapters
  DROP COLUMN IF EXISTS session_date,
  DROP COLUMN IF EXISTS duration_minutes,
  DROP COLUMN IF EXISTS location_name,
  DROP COLUMN IF EXISTS location_url,
  DROP COLUMN IF EXISTS method,
  DROP COLUMN IF EXISTS trainer_id;

-- 6. References. The composite FKs make a cross-project session impossible to insert.

ALTER TABLE lms_chapters DROP CONSTRAINT IF EXISTS lms_chapters_level_id_fkey;
ALTER TABLE lms_chapters DROP CONSTRAINT IF EXISTS lms_chapters_level_id_project_id_fkey;
ALTER TABLE lms_chapters
  ADD CONSTRAINT lms_chapters_level_id_project_id_fkey
  FOREIGN KEY (level_id, project_id) REFERENCES lms_levels (id, project_id);

ALTER TABLE lms_chapter_sessions DROP CONSTRAINT IF EXISTS lms_chapter_sessions_chapter_id_project_id_fkey;
ALTER TABLE lms_chapter_sessions
  ADD CONSTRAINT lms_chapter_sessions_chapter_id_project_id_fkey
  FOREIGN KEY (chapter_id, project_id) REFERENCES lms_chapters (id, project_id);

ALTER TABLE lms_chapter_sessions DROP CONSTRAINT IF EXISTS lms_chapter_sessions_only_group_id_project_id_fkey;
ALTER TABLE lms_chapter_sessions
  ADD CONSTRAINT lms_chapter_sessions_only_group_id_project_id_fkey
  FOREIGN KEY (only_group_id, project_id) REFERENCES lms_groups (id, project_id);

ALTER TABLE lms_chapter_sessions DROP CONSTRAINT IF EXISTS lms_chapter_sessions_trainer_id_fkey;
ALTER TABLE lms_chapter_sessions
  ADD CONSTRAINT lms_chapter_sessions_trainer_id_fkey
  FOREIGN KEY (trainer_id) REFERENCES trainers (id);

-- 7. level_number stays globally unique: lms_levels becomes a lookup -- see 2026-09-21-levels-lookup.sql.

-- 8. At most one all-groups session per chapter -- UNIQUE (chapter_id, only_group_id) lets NULLs repeat.

CREATE UNIQUE INDEX IF NOT EXISTS idx_lms_chapter_sessions_all_groups
  ON lms_chapter_sessions (chapter_id) WHERE only_group_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_lms_chapter_sessions_chapter_id    ON lms_chapter_sessions (chapter_id);
CREATE INDEX IF NOT EXISTS idx_lms_chapter_sessions_only_group_id ON lms_chapter_sessions (only_group_id);
CREATE INDEX IF NOT EXISTS idx_lms_chapter_sessions_trainer_id    ON lms_chapter_sessions (trainer_id);
CREATE INDEX IF NOT EXISTS idx_lms_chapters_project_id            ON lms_chapters (project_id);

DROP TRIGGER IF EXISTS update_lms_chapter_sessions_updated_at_trigger ON lms_chapter_sessions;
CREATE TRIGGER update_lms_chapter_sessions_updated_at_trigger
  BEFORE UPDATE ON lms_chapter_sessions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

COMMIT;
