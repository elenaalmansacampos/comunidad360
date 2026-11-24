INSERT INTO community (id, name, timezone, default_language, currency, status, created_at, updated_at)
SELECT gen_random_uuid(), 'Santo Tomás', 'Europe/Madrid', 'es', 'EUR', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM community WHERE name = 'Santo Tomás');

WITH new_community AS (
    SELECT id FROM community WHERE name = 'Santo Tomás' ORDER BY created_at ASC LIMIT 1
), admin_user AS (
    SELECT u.id
    FROM app_user u
    JOIN membership m ON m.user_id = u.id
    JOIN membership_role mr ON mr.membership_id = m.id AND mr.role_code = 'COMMUNITY_ADMIN'
    ORDER BY m.created_at ASC
    LIMIT 1
)
INSERT INTO membership (id, community_id, user_id, status, joined_at, created_at, updated_at)
SELECT gen_random_uuid(), c.id, a.id, 'ACTIVE', now(), now(), now()
FROM new_community c
CROSS JOIN admin_user a
WHERE NOT EXISTS (
    SELECT 1 FROM membership existing
    WHERE existing.community_id = c.id AND existing.user_id = a.id
);

WITH new_community AS (
    SELECT id FROM community WHERE name = 'Santo Tomás' ORDER BY created_at ASC LIMIT 1
), admin_user AS (
    SELECT u.id
    FROM app_user u
    JOIN membership m ON m.user_id = u.id
    JOIN membership_role mr ON mr.membership_id = m.id AND mr.role_code = 'COMMUNITY_ADMIN'
    ORDER BY m.created_at ASC
    LIMIT 1
), new_membership AS (
    SELECT m.id
    FROM membership m
    JOIN new_community c ON c.id = m.community_id
    JOIN admin_user a ON a.id = m.user_id
)
INSERT INTO membership_role (id, membership_id, role_code, assigned_at, assigned_by_user_id)
SELECT gen_random_uuid(), nm.id, 'COMMUNITY_ADMIN', now(), a.id
FROM new_membership nm
CROSS JOIN admin_user a
WHERE NOT EXISTS (
    SELECT 1 FROM membership_role existing
    WHERE existing.membership_id = nm.id AND existing.role_code = 'COMMUNITY_ADMIN'
);
