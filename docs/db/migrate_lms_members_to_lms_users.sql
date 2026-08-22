-- Migration: lms_members -> lms_users. Verified 0 rows in every affected table before writing this (DROP COLUMN / ALTER COLUMN TYPE below are not data-preserving) -- re-check row counts before rerunning against a populated Neon "Ailene" DB (orange-union-08059820).

BEGIN;

-- Drop FKs from dependent tables that point at lms_members (id) -- required before changing the id column's type
ALTER TABLE lms_groups               DROP CONSTRAINT lms_groups_champion_id_fkey;
ALTER TABLE lms_coaching_notes       DROP CONSTRAINT lms_coaching_notes_champion_id_fkey;
ALTER TABLE lms_coaching_notes       DROP CONSTRAINT lms_coaching_notes_member_id_fkey;
ALTER TABLE lms_material_completions DROP CONSTRAINT lms_material_completions_member_id_fkey;
ALTER TABLE lms_video_completions    DROP CONSTRAINT lms_video_completions_member_id_fkey;
ALTER TABLE lms_quiz_submissions     DROP CONSTRAINT lms_quiz_submissions_member_id_fkey;
ALTER TABLE lms_prompt_submissions   DROP CONSTRAINT lms_prompt_submissions_assigned_by_id_fkey;
ALTER TABLE lms_prompt_submissions   DROP CONSTRAINT lms_prompt_submissions_member_id_fkey;
ALTER TABLE lms_prompt_submissions   DROP CONSTRAINT lms_prompt_submissions_reviewed_by_id_fkey;
ALTER TABLE lms_use_case_submissions DROP CONSTRAINT lms_use_case_submissions_assigned_by_id_fkey;
ALTER TABLE lms_use_case_submissions DROP CONSTRAINT lms_use_case_submissions_member_id_fkey;
ALTER TABLE lms_use_case_submissions DROP CONSTRAINT lms_use_case_submissions_reviewed_by_id_fkey;
ALTER TABLE lms_xp_earnings          DROP CONSTRAINT lms_xp_earnings_member_id_fkey;
ALTER TABLE lms_pre_assessments      DROP CONSTRAINT lms_pre_assessments_member_id_fkey;

-- Reshape lms_members -> lms_users
ALTER TABLE lms_members DROP COLUMN user_id;
ALTER TABLE lms_members DROP COLUMN group_id;
ALTER TABLE lms_members DROP COLUMN current_level_id;
ALTER TABLE lms_members DROP COLUMN level_history;

ALTER TABLE lms_members ALTER COLUMN id DROP DEFAULT;
ALTER TABLE lms_members ALTER COLUMN id TYPE UUID USING NULL;
DROP SEQUENCE lms_members_id_seq;

ALTER TABLE lms_members ADD COLUMN full_name VARCHAR NOT NULL;
ALTER TABLE lms_members ADD COLUMN email VARCHAR NOT NULL UNIQUE;
ALTER TABLE lms_members ADD COLUMN avatar VARCHAR NULL;
ALTER TABLE lms_members ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE lms_members RENAME TO lms_users;
ALTER TABLE lms_users RENAME CONSTRAINT lms_members_pkey TO lms_users_pkey;

CREATE TRIGGER update_lms_users_updated_at_trigger
  BEFORE UPDATE ON lms_users
  FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- Switch the dependent tables' member/champion/reviewer columns to UUID
ALTER TABLE lms_groups               ALTER COLUMN champion_id     TYPE UUID USING NULL;
ALTER TABLE lms_coaching_notes       ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_coaching_notes       ALTER COLUMN champion_id     TYPE UUID USING NULL;
ALTER TABLE lms_material_completions ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_video_completions    ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_quiz_submissions     ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_prompt_submissions   ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_prompt_submissions   ALTER COLUMN assigned_by_id  TYPE UUID USING NULL;
ALTER TABLE lms_prompt_submissions   ALTER COLUMN reviewed_by_id  TYPE UUID USING NULL;
ALTER TABLE lms_use_case_submissions ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_use_case_submissions ALTER COLUMN assigned_by_id  TYPE UUID USING NULL;
ALTER TABLE lms_use_case_submissions ALTER COLUMN reviewed_by_id  TYPE UUID USING NULL;
ALTER TABLE lms_xp_earnings          ALTER COLUMN member_id       TYPE UUID USING NULL;
ALTER TABLE lms_pre_assessments      ALTER COLUMN member_id       TYPE UUID USING NULL;

-- Re-point the FKs at lms_users
ALTER TABLE lms_groups
  ADD FOREIGN KEY (champion_id) REFERENCES lms_users (id);
ALTER TABLE lms_coaching_notes
  ADD FOREIGN KEY (member_id)   REFERENCES lms_users (id),
  ADD FOREIGN KEY (champion_id) REFERENCES lms_users (id);
ALTER TABLE lms_material_completions
  ADD FOREIGN KEY (member_id) REFERENCES lms_users (id);
ALTER TABLE lms_video_completions
  ADD FOREIGN KEY (member_id) REFERENCES lms_users (id);
ALTER TABLE lms_quiz_submissions
  ADD FOREIGN KEY (member_id) REFERENCES lms_users (id);
ALTER TABLE lms_prompt_submissions
  ADD FOREIGN KEY (member_id)      REFERENCES lms_users (id),
  ADD FOREIGN KEY (assigned_by_id) REFERENCES lms_users (id),
  ADD FOREIGN KEY (reviewed_by_id) REFERENCES lms_users (id);
ALTER TABLE lms_use_case_submissions
  ADD FOREIGN KEY (member_id)      REFERENCES lms_users (id),
  ADD FOREIGN KEY (assigned_by_id) REFERENCES lms_users (id),
  ADD FOREIGN KEY (reviewed_by_id) REFERENCES lms_users (id);
ALTER TABLE lms_xp_earnings
  ADD FOREIGN KEY (member_id) REFERENCES lms_users (id);
ALTER TABLE lms_pre_assessments
  ADD FOREIGN KEY (member_id) REFERENCES lms_users (id);

COMMIT;
