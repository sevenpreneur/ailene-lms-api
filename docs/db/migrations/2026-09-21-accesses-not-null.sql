-- lms_accesses.group_id and current_level_id become mandatory. Guarded, re-runnable.

BEGIN;

UPDATE lms_accesses a
   SET group_id = (SELECT g.id FROM lms_groups g WHERE g.project_id = a.project_id ORDER BY g.id LIMIT 1)
 WHERE a.group_id IS NULL;

UPDATE lms_accesses
   SET current_level_id = (SELECT id FROM lms_levels ORDER BY level_number LIMIT 1)
 WHERE current_level_id IS NULL;

ALTER TABLE lms_accesses ALTER COLUMN group_id SET NOT NULL;
ALTER TABLE lms_accesses ALTER COLUMN current_level_id SET DEFAULT 1;
ALTER TABLE lms_accesses ALTER COLUMN current_level_id SET NOT NULL;

COMMIT;
