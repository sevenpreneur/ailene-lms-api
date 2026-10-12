-- Single pre-assessment per access (no post stage), one flat target band, attempt status reduced to submitted/scored

-- 1. Drop the post stage

ALTER TABLE lms_pa_attempts DROP COLUMN stage;
ALTER TABLE lms_pa_attempts ADD UNIQUE (access_id);
ALTER TABLE lms_pa_attempts ADD FOREIGN KEY (form_id) REFERENCES lms_pa_forms (id);
ALTER TABLE lms_pa_forms DROP COLUMN stage;
DROP TYPE lms_pa_stage_enum;
CREATE UNIQUE INDEX idx_lms_pa_forms_active ON lms_pa_forms ((TRUE)) WHERE status = 'active';

-- 2. One flat target band instead of per job level and role

DROP TABLE lms_pa_target_clusters;
DROP TABLE lms_pa_targets;
ALTER TABLE lms_accesses DROP COLUMN job_level;
DROP TYPE lms_job_level_enum;
ALTER TABLE lms_pa_scoring_configs ADD COLUMN target_band SMALLINT NOT NULL DEFAULT 2 CHECK (target_band BETWEEN 0 AND 5);
ALTER TABLE lms_pa_attempts DROP COLUMN target_band;

-- 3. Attempt status: submitted until score computes it, then scored

ALTER TABLE lms_pa_attempts DROP COLUMN error_message;
ALTER TABLE lms_pa_attempts ALTER COLUMN status DROP DEFAULT;
ALTER TYPE lms_pa_attempt_status_enum RENAME TO lms_pa_attempt_status_enum_old;
CREATE TYPE lms_pa_attempt_status_enum AS ENUM ('submitted', 'scored');
ALTER TABLE lms_pa_attempts ALTER COLUMN status TYPE lms_pa_attempt_status_enum
  USING (CASE WHEN status::text = 'scored' THEN 'scored' ELSE 'submitted' END)::lms_pa_attempt_status_enum;
ALTER TABLE lms_pa_attempts ALTER COLUMN status SET DEFAULT 'submitted';
DROP TYPE lms_pa_attempt_status_enum_old;
