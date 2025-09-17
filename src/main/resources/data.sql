SET FOREIGN_KEY_CHECKS = 0;


DELETE FROM onboarding_cases;
DELETE FROM user_roles;
DELETE FROM users;
DELETE FROM roles;


SET FOREIGN_KEY_CHECKS = 1;


INSERT INTO roles (name) VALUES ('SYSTEM_ADMINISTRATOR');
INSERT INTO roles (name) VALUES ('ONBOARDING_OFFICER');
INSERT INTO roles (name) VALUES ('COMPLIANCE_MANAGER');


INSERT INTO users (username, password, full_name, email, enabled, created_at) VALUES 
('admin', 'password123', 'System Administrator', 'admin@company.com', true, NOW()),
('officer1', 'password123', 'John Officer', 'officer@company.com', true, NOW()),
('manager1', 'password123', 'Jane Manager', 'manager@company.com', true, NOW());


INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'SYSTEM_ADMINISTRATOR';
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'officer1' AND r.name = 'ONBOARDING_OFFICER';
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'manager1' AND r.name = 'COMPLIANCE_MANAGER';
