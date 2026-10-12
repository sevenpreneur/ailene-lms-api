-- Pre-assessment forms become shared versions, and each project is pointed at exactly one version

-- 1. Rename lms_pa_forms to lms_pa_versions; the project link replaces the active flag

DROP INDEX idx_lms_pa_forms_active;
ALTER TABLE lms_pa_forms DROP COLUMN status, DROP COLUMN published_at;
ALTER TABLE lms_pa_forms RENAME TO lms_pa_versions;
ALTER SEQUENCE lms_pa_forms_id_seq RENAME TO lms_pa_versions_id_seq;
ALTER TABLE lms_pa_versions RENAME CONSTRAINT lms_pa_forms_pkey TO lms_pa_versions_pkey;
ALTER TABLE lms_pa_versions RENAME CONSTRAINT lms_pa_forms_code_version_key TO lms_pa_versions_code_version_key;
ALTER TRIGGER update_lms_pa_forms_updated_at_trigger ON lms_pa_versions RENAME TO update_lms_pa_versions_updated_at_trigger;

-- 2. form_id becomes version_id

ALTER TABLE lms_pa_items RENAME COLUMN form_id TO version_id;
ALTER TABLE lms_pa_items RENAME CONSTRAINT lms_pa_items_form_id_fkey TO lms_pa_items_version_id_fkey;
ALTER TABLE lms_pa_items RENAME CONSTRAINT lms_pa_items_form_id_item_code_key TO lms_pa_items_version_id_item_code_key;
ALTER TABLE lms_pa_items RENAME CONSTRAINT lms_pa_items_id_form_id_key TO lms_pa_items_id_version_id_key;
ALTER TABLE lms_pa_items RENAME CONSTRAINT lms_pa_items_overclaim_item_id_form_id_fkey TO lms_pa_items_overclaim_item_id_version_id_fkey;
ALTER TABLE lms_pa_attempts RENAME COLUMN form_id TO version_id;
ALTER TABLE lms_pa_attempts RENAME CONSTRAINT lms_pa_attempts_form_id_fkey TO lms_pa_attempts_version_id_fkey;
ALTER TABLE lms_pa_attempts RENAME CONSTRAINT lms_pa_attempts_id_form_id_key TO lms_pa_attempts_id_version_id_key;
ALTER INDEX idx_lms_pa_attempts_form_id RENAME TO idx_lms_pa_attempts_version_id;
ALTER TABLE lms_pa_answers RENAME COLUMN form_id TO version_id;
ALTER TABLE lms_pa_answers RENAME CONSTRAINT lms_pa_answers_attempt_id_form_id_fkey TO lms_pa_answers_attempt_id_version_id_fkey;
ALTER TABLE lms_pa_answers RENAME CONSTRAINT lms_pa_answers_item_id_form_id_fkey TO lms_pa_answers_item_id_version_id_fkey;

-- 3. One version per project

CREATE TABLE lms_pa_project_versions (
  project_id  CHAR(21)     PRIMARY KEY,
  version_id  INTEGER      NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMPTZ  NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE lms_pa_project_versions
  ADD FOREIGN KEY (project_id) REFERENCES lms_projects (id),
  ADD FOREIGN KEY (version_id) REFERENCES lms_pa_versions (id);

CREATE INDEX idx_lms_pa_project_versions_version_id ON lms_pa_project_versions (version_id);

CREATE TRIGGER update_lms_pa_project_versions_updated_at_trigger
  BEFORE UPDATE ON lms_pa_project_versions
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 4. Point every existing project at Form A v1

INSERT INTO lms_pa_project_versions (project_id, version_id)
SELECT p.id, v.id FROM lms_projects p CROSS JOIN lms_pa_versions v WHERE v.code = 'A' AND v.version = 1;
