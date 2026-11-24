INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'MEMBER_INVITE', true),
    ('COMMUNITY_ADMIN', 'MEMBER_INVITE', true),
    ('PRESIDENT', 'MEMBER_INVITE', true),
    ('SECRETARY', 'MEMBER_INVITE', true)
ON CONFLICT (role_code, permission_code) DO UPDATE SET granted = true;
