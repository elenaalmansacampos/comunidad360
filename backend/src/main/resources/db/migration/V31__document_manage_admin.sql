INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'DOCUMENT_MANAGE', true),
    ('COMMUNITY_ADMIN', 'DOCUMENT_MANAGE', true),
    ('PRESIDENT', 'DOCUMENT_MANAGE', true),
    ('SECRETARY', 'DOCUMENT_MANAGE', true)
ON CONFLICT (role_code, permission_code) DO UPDATE SET granted = true;
