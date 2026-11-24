WITH new_community AS (
    SELECT id FROM community WHERE name = 'Santo Tomás' ORDER BY created_at ASC LIMIT 1
), target_user AS (
    SELECT id FROM app_user WHERE lower(email) = 'admin@comunidad360.local' LIMIT 1
)
INSERT INTO membership (id, community_id, user_id, status, joined_at, created_at, updated_at)
SELECT gen_random_uuid(), c.id, u.id, 'ACTIVE', now(), now(), now()
FROM new_community c
CROSS JOIN target_user u
WHERE NOT EXISTS (
    SELECT 1 FROM membership existing
    WHERE existing.community_id = c.id AND existing.user_id = u.id
);

WITH new_community AS (
    SELECT id FROM community WHERE name = 'Santo Tomás' ORDER BY created_at ASC LIMIT 1
), target_user AS (
    SELECT id FROM app_user WHERE lower(email) = 'admin@comunidad360.local' LIMIT 1
)
UPDATE membership
SET status = 'ACTIVE', updated_at = now()
FROM new_community c, target_user u
WHERE membership.community_id = c.id AND membership.user_id = u.id;

WITH new_community AS (
    SELECT id FROM community WHERE name = 'Santo Tomás' ORDER BY created_at ASC LIMIT 1
), target_user AS (
    SELECT id FROM app_user WHERE lower(email) = 'admin@comunidad360.local' LIMIT 1
), target_membership AS (
    SELECT m.id
    FROM membership m
    JOIN new_community c ON c.id = m.community_id
    JOIN target_user u ON u.id = m.user_id
)
INSERT INTO membership_role (id, membership_id, role_code, assigned_at, assigned_by_user_id)
SELECT gen_random_uuid(), tm.id, 'COMMUNITY_ADMIN', now(), u.id
FROM target_membership tm
CROSS JOIN target_user u
WHERE NOT EXISTS (
    SELECT 1 FROM membership_role existing
    WHERE existing.membership_id = tm.id AND existing.role_code = 'COMMUNITY_ADMIN'
);
