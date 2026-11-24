INSERT INTO role (code, name_key, scope, is_system) VALUES
    ('VECINO', 'role.vecino', 'COMMUNITY', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('VECINO', 'COMMUNITY_VIEW', true),
    ('VECINO', 'MEMBER_VIEW', true),
    ('VECINO', 'UNIT_VIEW', true),
    ('VECINO', 'FINANCE_VIEW', true),
    ('VECINO', 'REPORT_VIEW', true),
    ('VECINO', 'BANK_ACCOUNT_VIEW', true),
    ('VECINO', 'INCIDENT_VIEW_ALL', true),
    ('VECINO', 'DOCUMENT_VIEW', true),
    ('VECINO', 'MEETING_VIEW', true),
    ('VECINO', 'VOTE_VIEW', true),
    ('VECINO', 'CHAT_VIEW', true),
    ('VECINO', 'PROVIDER_VIEW', true),
    ('VECINO', 'LOST_ITEM_VIEW', true),
    ('VECINO', 'BOARD_VIEW', true),
    ('VECINO', 'WORK_PERMIT_VIEW', true),

    ('VECINO', 'INCIDENT_CREATE', true),
    ('VECINO', 'INCIDENT_COMMENT', true),
    ('VECINO', 'CHAT_SEND', true),
    ('VECINO', 'LOST_ITEM_CREATE', true),
    ('VECINO', 'BOARD_CREATE', true)
ON CONFLICT DO NOTHING;
