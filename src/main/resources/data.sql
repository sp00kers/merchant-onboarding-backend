-- Initial Data Seeding Script
-- This script uses INSERT IGNORE to only insert data if it doesn't already exist
-- To fully reset the database, manually delete the data first or drop the tables

SET FOREIGN_KEY_CHECKS = 0;

-- NOTE: DELETE statements are commented out to preserve data between restarts
-- Uncomment the following lines ONLY if you want to reset all data:
-- DELETE FROM case_history;
-- DELETE FROM documents;
-- DELETE FROM onboarding_cases;
-- DELETE FROM users;
-- DELETE FROM role_permissions;
-- DELETE FROM roles;
-- DELETE FROM permissions;
-- DELETE FROM business_types;
-- DELETE FROM merchant_categories;

SET FOREIGN_KEY_CHECKS = 1;

-- Insert Permissions (IGNORE if already exists)
INSERT IGNORE INTO permissions (id, name, description, category, is_active, created_at, updated_at) VALUES
('all_modules', 'All Modules', 'Access to all system modules', 'system', true, NOW(), NOW()),
('user_management', 'User Management', 'Manage system users', 'user', true, NOW(), NOW()),
('system_configuration', 'System Configuration', 'Configure system settings', 'system', true, NOW(), NOW()),
('role_management', 'Role Management', 'Manage roles and permissions', 'role', true, NOW(), NOW()),
('permission_management', 'Permission Management', 'Manage system permissions', 'role', true, NOW(), NOW()),
('case_creation', 'Case Creation', 'Create new cases', 'case', true, NOW(), NOW()),
('case_view', 'Case View', 'View cases in read-only mode', 'case', true, NOW(), NOW()),
('case_management', 'Case Management', 'Edit and manage existing cases', 'case', true, NOW(), NOW()),
('document_upload', 'Document Upload', 'Upload case documents', 'case', true, NOW(), NOW()),
('compliance_check', 'Compliance Check', 'Perform compliance checks', 'case', true, NOW(), NOW()),
('risk_assessment', 'Risk Assessment', 'Conduct risk assessments', 'case', true, NOW(), NOW()),
('background_check', 'Background Check', 'Perform background verifications', 'case', true, NOW(), NOW()),
('external_api_access', 'External API Access', 'Access external APIs', 'system', true, NOW(), NOW()),
('verification_reports', 'Verification Reports', 'Generate verification reports', 'report', true, NOW(), NOW()),
('audit_view', 'Audit View', 'View audit logs and system activity', 'system', true, NOW(), NOW());

-- Insert Roles (IGNORE if already exists)
INSERT IGNORE INTO roles (id, name, description, is_active, created_at, updated_at) VALUES
('admin', 'System Administrator', 'Full system access and user management', true, NOW(), NOW()),
('onboarding_officer', 'Onboarding Officer', 'Create new merchant onboarding cases and view existing ones', true, NOW(), NOW()),
('compliance_reviewer', 'Compliance Reviewer', 'Review and edit cases for regulatory compliance', true, NOW(), NOW());

-- Assign permissions to roles (IGNORE if already exists)
-- Admin gets all permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
('admin', 'all_modules'),
('admin', 'user_management'),
('admin', 'system_configuration'),
('admin', 'role_management'),
('admin', 'permission_management'),
('admin', 'audit_view');

-- Onboarding Officer permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
('onboarding_officer', 'case_creation'),
('onboarding_officer', 'case_view'),
('onboarding_officer', 'document_upload');

-- Compliance Reviewer permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
('compliance_reviewer', 'case_view'),
('compliance_reviewer', 'case_management'),
('compliance_reviewer', 'compliance_check'),
('compliance_reviewer', 'risk_assessment'),
('compliance_reviewer', 'document_upload'),
('compliance_reviewer', 'system_configuration');

-- Insert Users (password is 'password123' encoded with BCrypt) - IGNORE if already exists
INSERT IGNORE INTO users (id, name, email, password, role_id, department, phone, status, last_login, notes, created_at, updated_at) VALUES
('USR001', 'John Doe', 'john.doe@bank.com', '$2a$10$G3WbYrDSBzXZb6deZEcKxeiNxu2qXyRL9M3mfTkmlv8IK.usAJpmW', 'onboarding_officer', 'Merchant Services', '+60123456789', 'active', NOW(), 'Senior officer with 5 years experience', NOW(), NOW()),
('USR002', 'Jane Smith', 'jane.smith@bank.com', '$2a$10$G3WbYrDSBzXZb6deZEcKxeiNxu2qXyRL9M3mfTkmlv8IK.usAJpmW', 'compliance_reviewer', 'Compliance', '+60123456788', 'active', NOW(), 'Compliance specialist', NOW(), NOW()),
('USR004', 'Sarah Lee', 'sarah.lee@bank.com', '$2a$10$G3WbYrDSBzXZb6deZEcKxeiNxu2qXyRL9M3mfTkmlv8IK.usAJpmW', 'admin', 'IT', '+60123456786', 'active', NOW(), 'System administrator', NOW(), NOW()),
('USR005', 'David Chen', 'david.chen@bank.com', '$2a$10$G3WbYrDSBzXZb6deZEcKxeiNxu2qXyRL9M3mfTkmlv8IK.usAJpmW', 'onboarding_officer', 'Operations', '+60123456785', 'inactive', NULL, 'On leave', NOW(), NOW());

-- Insert Business Types (IGNORE if already exists)
INSERT IGNORE INTO business_types (id, code, name, description, status, created_at, updated_at) VALUES
('bt1', 'SP', 'Sole Proprietorship', 'Individual business ownership', 'active', NOW(), NOW()),
('bt2', 'PT', 'Partnership', 'Business partnership between two or more individuals', 'active', NOW(), NOW()),
('bt3', 'SB', 'Sdn Bhd', 'Private limited company', 'active', NOW(), NOW()),
('bt4', 'BHD', 'Bhd', 'Public limited company', 'active', NOW(), NOW());

-- Insert Merchant Categories (IGNORE if already exists)
INSERT IGNORE INTO merchant_categories (id, code, name, description, risk_level, status, created_at, updated_at) VALUES
('mc1', 'RET', 'Retail', 'Physical retail stores and outlets', 'low', 'active', NOW(), NOW()),
('mc2', 'ECM', 'E-commerce', 'Online retail and digital commerce', 'medium', 'active', NOW(), NOW()),
('mc3', 'F&B', 'Food & Beverage', 'Restaurants, cafes, and food services', 'low', 'active', NOW(), NOW()),
('mc4', 'SRV', 'Services', 'Professional and business services', 'medium', 'active', NOW(), NOW());
