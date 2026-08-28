-- Run explicitly against an existing TrustDeck database after deploying the
-- ENTITY_TYPE permission scope. The script is safe to rerun.
BEGIN;

WITH project_entity_grants AS (
    SELECT pg.subject_id, et.id AS entity_type_id, pg.action, pg.decision,
           pg.valid_from, pg.valid_to, pg.created_at, pg.created_by,
           pg.updated_at, pg.updated_by
    FROM permission_grant pg
    JOIN entity_type et ON et.project_id = pg.resource_id
    WHERE pg.resource_type = 'PROJECT'
      AND pg.action IN ('entity:create', 'entity:read', 'entity:update', 'entity:delete',
                        'entity:search', 'entity:record-linkage', 'entity:resolve-linkage',
                        'entity:list-pseudonyms')
       -- Preserve both current and scheduled grants, including their decision
       -- and validity window, before removing the obsolete project scope.
)
INSERT INTO permission_grant (subject_id, resource_type, resource_id, action, decision,
                              valid_from, valid_to, created_at, created_by, updated_at, updated_by)
SELECT subject_id, 'ENTITY_TYPE', entity_type_id, action, decision,
       valid_from, valid_to, created_at, created_by, updated_at, updated_by
FROM project_entity_grants
ON CONFLICT (subject_id, resource_type, resource_id, action) DO NOTHING;

-- Legacy project-scoped entity actions must not remain effective.
DELETE FROM permission_grant
WHERE resource_type = 'PROJECT'
  AND action IN ('entity:create', 'entity:read', 'entity:update', 'entity:delete',
                 'entity:search', 'entity:record-linkage', 'entity:resolve-linkage',
                 'entity:list-pseudonyms');

COMMIT;
