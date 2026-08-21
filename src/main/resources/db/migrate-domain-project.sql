-- Assign domains indirectly referenced by entity types to their sole owning project.
-- Domains without an unambiguous entity-type owner are assigned to HDP-Kafka.

BEGIN;

ALTER TABLE public.domain ADD COLUMN IF NOT EXISTS project_id integer;

DO $$
DECLARE
    fallback_project_id integer;
BEGIN
    SELECT id INTO fallback_project_id
    FROM public.project
    WHERE abbreviation = 'HDP-Kafka';

    IF fallback_project_id IS NULL THEN
        RAISE EXCEPTION 'Project with abbreviation HDP-Kafka is required for domain migration';
    END IF;

    -- Only a unique entity-type project reference is safe to infer as ownership.
    WITH entity_type_ownership AS (
        SELECT associated_domain_id AS domain_id, MIN(project_id) AS project_id
        FROM public.entity_type
        WHERE associated_domain_id IS NOT NULL AND project_id IS NOT NULL
        GROUP BY associated_domain_id
        HAVING COUNT(DISTINCT project_id) = 1
    )
    UPDATE public.domain AS domain
    SET project_id = ownership.project_id
    FROM entity_type_ownership AS ownership
    WHERE domain.id = ownership.domain_id AND domain.project_id IS NULL;

    UPDATE public.domain
    SET project_id = fallback_project_id
    WHERE project_id IS NULL;
END
$$;

ALTER TABLE public.domain ALTER COLUMN project_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'domain_project_id_fkey'
          AND conrelid = 'public.domain'::regclass
    ) THEN
        ALTER TABLE public.domain
            ADD CONSTRAINT domain_project_id_fkey
                FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE RESTRICT;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS domain_project_id_idx ON public.domain (project_id);

COMMIT;
