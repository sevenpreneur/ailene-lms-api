-- Turn lms_levels into a global lookup. Run AFTER 2026-09-21-chapter-sessions.sql. Guarded, re-runnable.

BEGIN;

-- 1. Must precede step 3: until this exists, level_id is the libraries' only tenant boundary. NULL = shared.

ALTER TABLE lms_prompts   ADD COLUMN IF NOT EXISTS only_project_id CHAR(21) NULL;
ALTER TABLE lms_use_cases ADD COLUMN IF NOT EXISTS only_project_id CHAR(21) NULL;

UPDATE lms_prompts p
   SET only_project_id = lv.project_id
  FROM lms_levels lv
 WHERE lv.id = p.level_id AND p.only_project_id IS NULL;

UPDATE lms_use_cases u
   SET only_project_id = lv.project_id
  FROM lms_levels lv
 WHERE lv.id = u.level_id AND u.only_project_id IS NULL;

ALTER TABLE lms_prompts   DROP CONSTRAINT IF EXISTS lms_prompts_only_project_id_fkey;
ALTER TABLE lms_prompts
  ADD CONSTRAINT lms_prompts_only_project_id_fkey
  FOREIGN KEY (only_project_id) REFERENCES lms_projects (id);

ALTER TABLE lms_use_cases DROP CONSTRAINT IF EXISTS lms_use_cases_only_project_id_fkey;
ALTER TABLE lms_use_cases
  ADD CONSTRAINT lms_use_cases_only_project_id_fkey
  FOREIGN KEY (only_project_id) REFERENCES lms_projects (id);

CREATE INDEX IF NOT EXISTS idx_lms_prompts_only_project_id   ON lms_prompts (only_project_id);
CREATE INDEX IF NOT EXISTS idx_lms_use_cases_only_project_id ON lms_use_cases (only_project_id);

-- 2. Composite FKs into lms_levels (id, project_id) cannot survive the drop, and mean nothing for a lookup.

ALTER TABLE lms_chapters DROP CONSTRAINT IF EXISTS lms_chapters_level_id_project_id_fkey;
ALTER TABLE lms_chapters DROP CONSTRAINT IF EXISTS lms_chapters_level_id_fkey;
ALTER TABLE lms_chapters
  ADD CONSTRAINT lms_chapters_level_id_fkey
  FOREIGN KEY (level_id) REFERENCES lms_levels (id);

ALTER TABLE lms_accesses DROP CONSTRAINT IF EXISTS lms_accesses_current_level_id_project_id_fkey;
ALTER TABLE lms_accesses DROP CONSTRAINT IF EXISTS lms_accesses_current_level_id_fkey;
ALTER TABLE lms_accesses
  ADD CONSTRAINT lms_accesses_current_level_id_fkey
  FOREIGN KEY (current_level_id) REFERENCES lms_levels (id);

-- 3. Drop the column, and the UNIQUE that only existed to support those composite FKs.

ALTER TABLE lms_levels DROP CONSTRAINT IF EXISTS lms_levels_id_project_id_key;
ALTER TABLE lms_levels DROP CONSTRAINT IF EXISTS lms_levels_project_id_fkey;
ALTER TABLE lms_levels DROP COLUMN IF EXISTS project_id;

-- lms_levels_level_number_key is kept on purpose: it is the lookup key findByLevelNumber(2) relies on.

COMMIT;
