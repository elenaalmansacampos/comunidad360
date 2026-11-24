INSERT INTO role (code, name_key, scope, is_system) VALUES
    ('VIEWER', 'role.viewer', 'COMMUNITY', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO permission (code, description_key, module) VALUES
    ('INCIDENT_COMMENT', 'permission.incidentComment', 'incidents'),
    ('CHAT_SEND', 'permission.chatSend', 'chat'),
    ('LOST_ITEM_CREATE', 'permission.lostItemCreate', 'lostfound')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'INCIDENT_COMMENT', true),
    ('SUPER_ADMIN', 'CHAT_SEND', true),
    ('SUPER_ADMIN', 'LOST_ITEM_CREATE', true),

    ('COMMUNITY_ADMIN', 'INCIDENT_COMMENT', true),
    ('COMMUNITY_ADMIN', 'CHAT_SEND', true),
    ('COMMUNITY_ADMIN', 'LOST_ITEM_CREATE', true),

    ('PRESIDENT', 'INCIDENT_COMMENT', true),
    ('PRESIDENT', 'CHAT_SEND', true),
    ('PRESIDENT', 'LOST_ITEM_CREATE', true),

    ('SECRETARY', 'INCIDENT_COMMENT', true),
    ('SECRETARY', 'CHAT_SEND', true),
    ('SECRETARY', 'LOST_ITEM_CREATE', true),

    ('TREASURER', 'INCIDENT_COMMENT', true),
    ('TREASURER', 'CHAT_SEND', true),
    ('TREASURER', 'LOST_ITEM_CREATE', true),

    ('OWNER', 'INCIDENT_COMMENT', true),
    ('OWNER', 'CHAT_SEND', true),
    ('OWNER', 'LOST_ITEM_CREATE', true),

    ('TENANT', 'INCIDENT_COMMENT', true),
    ('TENANT', 'CHAT_SEND', true),
    ('TENANT', 'LOST_ITEM_CREATE', true),

    ('PROVIDER', 'INCIDENT_COMMENT', true),
    ('PROVIDER', 'LOST_ITEM_CREATE', true)
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('VIEWER', 'COMMUNITY_VIEW', true),
    ('VIEWER', 'MEMBER_VIEW', true),
    ('VIEWER', 'UNIT_VIEW', true),
    ('VIEWER', 'FINANCE_VIEW', true),
    ('VIEWER', 'REPORT_VIEW', true),
    ('VIEWER', 'BANK_ACCOUNT_VIEW', true),
    ('VIEWER', 'INCIDENT_VIEW_ALL', true),
    ('VIEWER', 'DOCUMENT_VIEW', true),
    ('VIEWER', 'MEETING_VIEW', true),
    ('VIEWER', 'VOTE_VIEW', true),
    ('VIEWER', 'CHAT_VIEW', true),
    ('VIEWER', 'PROVIDER_VIEW', true),
    ('VIEWER', 'LOST_ITEM_VIEW', true),
    ('VIEWER', 'BOARD_VIEW', true)
ON CONFLICT DO NOTHING;
