-- Run explicitly against an existing TrustDeck database after deploying the
-- statistics endpoints. The script is safe to rerun.
BEGIN;

-- Replace the obsolete global table-storage grant while preserving its metadata.
INSERT INTO permission_grant
(
    subject_id, resource_type, resource_id, action, decision,
    valid_from, valid_to, created_at, created_by, updated_at, updated_by
)
SELECT
    subject_id, 'GLOBAL', resource_id, 'system:statistics', decision,
    valid_from, valid_to, created_at, created_by, updated_at, updated_by
FROM permission_grant
WHERE resource_type = 'GLOBAL'
  AND action = 'table:read-storage'
ON CONFLICT (subject_id, resource_type, resource_id, action) DO NOTHING;

DELETE FROM permission_grant
WHERE resource_type = 'GLOBAL'
  AND action = 'table:read-storage';

-- Grant project statistics to existing project permission managers only.
INSERT INTO permission_grant
(
    subject_id, resource_type, resource_id, action, decision,
    valid_from, valid_to, created_at, created_by, updated_at, updated_by
)
SELECT
    subject_id, 'PROJECT', resource_id, 'project:statistics', decision,
    valid_from, valid_to, created_at, created_by, updated_at, updated_by
FROM permission_grant
WHERE resource_type = 'PROJECT'
  AND action = 'project:manage-permissions'
ON CONFLICT (subject_id, resource_type, resource_id, action) DO NOTHING;

COMMIT;

-- Ordinary project users must receive project:statistics explicitly when desired.
