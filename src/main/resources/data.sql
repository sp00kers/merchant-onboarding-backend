SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing data
DELETE FROM case_history;
DELETE FROM documents;
DELETE FROM onboarding_cases;
DELETE FROM users;
DELETE FROM role_permissions;
DELETE FROM roles;
DELETE FROM permissions;
DELETE FROM business_types;
DELETE FROM merchant_categories;
DELETE FROM risk_categories;

SET FOREIGN_KEY_CHECKS = 1;

-- Insert Permissions
INSERT INTO permissions (id, name, description, category, is_active, created_at, updated_at) VALUES
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
('verification_reports', 'Verification Reports', 'Generate verification reports', 'report', true, NOW(), NOW());

-- Insert Roles
INSERT INTO roles (id, name, description, is_active, created_at, updated_at) VALUES
('admin', 'System Administrator', 'Full system access and user management', true, NOW(), NOW()),
('onboarding_officer', 'Onboarding Officer', 'Create new merchant onboarding cases and view existing ones', true, NOW(), NOW()),
('compliance_reviewer', 'Compliance Reviewer', 'Review and edit cases for regulatory compliance', true, NOW(), NOW()),
('verifier', 'Background Verifier', 'Conduct background verification processes', true, NOW(), NOW());

-- Assign permissions to roles
-- Admin gets all permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('admin', 'all_modules'),
('admin', 'user_management'),
('admin', 'system_configuration'),
('admin', 'role_management'),
('admin', 'permission_management');

-- Onboarding Officer permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('onboarding_officer', 'case_creation'),
('onboarding_officer', 'case_view'),
('onboarding_officer', 'document_upload');

-- Compliance Reviewer permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('compliance_reviewer', 'case_view'),
('compliance_reviewer', 'case_management'),
('compliance_reviewer', 'compliance_check'),
('compliance_reviewer', 'risk_assessment'),
('compliance_reviewer', 'document_upload');

-- Verifier permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('verifier', 'case_view'),
('verifier', 'background_check'),
('verifier', 'external_api_access'),
('verifier', 'verification_reports');

-- Insert Users (password is 'password123' encoded with BCrypt)
INSERT INTO users (id, name, email, password, role_id, department, phone, status, last_login, notes, created_at, updated_at) VALUES
('USR001', 'John Doe', 'john.doe@bank.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqVlNzXhbLGsqVk8VZZ3sLkRm.auVEa', 'onboarding_officer', 'Merchant Services', '+60123456789', 'active', NOW(), 'Senior officer with 5 years experience', NOW(), NOW()),
('USR002', 'Jane Smith', 'jane.smith@bank.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqVlNzXhbLGsqVk8VZZ3sLkRm.auVEa', 'compliance_reviewer', 'Compliance', '+60123456788', 'active', NOW(), 'Compliance specialist', NOW(), NOW()),
('USR003', 'Mike Johnson', 'mike.johnson@bank.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqVlNzXhbLGsqVk8VZZ3sLkRm.auVEa', 'verifier', 'Risk Management', '+60123456787', 'active', NOW(), 'Background verification expert', NOW(), NOW()),
('USR004', 'Sarah Lee', 'sarah.lee@bank.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqVlNzXhbLGsqVk8VZZ3sLkRm.auVEa', 'admin', 'IT', '+60123456786', 'active', NOW(), 'System administrator', NOW(), NOW()),
('USR005', 'David Chen', 'david.chen@bank.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqVlNzXhbLGsqVk8VZZ3sLkRm.auVEa', 'onboarding_officer', 'Operations', '+60123456785', 'inactive', NULL, 'On leave', NOW(), NOW());

-- Insert Business Types
INSERT INTO business_types (id, code, name, description, status, created_at, updated_at) VALUES
('bt1', 'SP', 'Sole Proprietorship', 'Individual business ownership', 'active', NOW(), NOW()),
('bt2', 'PT', 'Partnership', 'Business partnership between two or more individuals', 'active', NOW(), NOW()),
('bt3', 'SB', 'Sdn Bhd', 'Private limited company', 'active', NOW(), NOW()),
('bt4', 'BHD', 'Bhd', 'Public limited company', 'active', NOW(), NOW());

-- Insert Merchant Categories
INSERT INTO merchant_categories (id, code, name, description, risk_level, status, created_at, updated_at) VALUES
('mc1', 'RET', 'Retail', 'Physical retail stores and outlets', 'low', 'active', NOW(), NOW()),
('mc2', 'ECM', 'E-commerce', 'Online retail and digital commerce', 'medium', 'active', NOW(), NOW()),
('mc3', 'F&B', 'Food & Beverage', 'Restaurants, cafes, and food services', 'low', 'active', NOW(), NOW()),
('mc4', 'SRV', 'Services', 'Professional and business services', 'medium', 'active', NOW(), NOW());

-- Insert Risk Categories
INSERT INTO risk_categories (id, level, name, score_range, description, actions_required, created_at, updated_at) VALUES
('rc1', 1, 'Low Risk', '0-30', 'Minimal risk requiring standard verification', 'Standard document verification and basic due diligence', NOW(), NOW()),
('rc2', 2, 'Medium Risk', '31-70', 'Moderate risk requiring enhanced verification', 'Enhanced due diligence, additional documentation, senior approval required', NOW(), NOW()),
('rc3', 3, 'High Risk', '71-100', 'High risk requiring comprehensive assessment', 'Comprehensive due diligence, management approval, ongoing monitoring', NOW(), NOW());

-- Insert Sample Cases
INSERT INTO onboarding_cases (case_id, business_name, business_type, registration_number, merchant_category, business_address, director_name, director_ic, director_phone, director_email, status, created_date, assigned_to, priority, last_updated, created_at, updated_at) VALUES
('MOP-2024-001', 'ABC Electronics Sdn Bhd', 'Sdn Bhd', '201801012345', 'Retail', '123 Technology Park, Cyberjaya, Selangor, Malaysia', 'Ahmad bin Abdullah', '850123-14-5678', '+60123456789', 'ahmad@abcelectronics.com', 'Pending Review', '2024-01-15', 'John Doe', 'Normal', '2024-01-15 14:20', NOW(), NOW()),
('MOP-2024-002', 'XYZ Trading', 'Partnership', '201901054321', 'E-commerce', '456 Business Park, Kuala Lumpur, Malaysia', 'Lim Wei Chen', '880512-10-1234', '+60198765432', 'lim@xyztrading.com', 'Background Verification', '2024-01-14', 'Jane Smith', 'Normal', '2024-01-14 16:00', NOW(), NOW()),
('MOP-2024-003', 'Tech Solutions Ltd', 'Bhd', '201701098765', 'Services', '789 Innovation Hub, Penang, Malaysia', 'Raj Kumar', '790815-07-9876', '+60171234567', 'raj@techsolutions.com', 'Approved', '2024-01-13', 'Mike Johnson', 'Normal', '2024-01-13 11:30', NOW(), NOW()),
('MOP-2024-004', 'Digital Marketing Co', 'Sdn Bhd', '202001067890', 'Services', '321 Digital Avenue, Johor Bahru, Malaysia', 'Siti Aminah', '910303-01-5432', '+60131234567', 'siti@digitalmarketing.com', 'Compliance Review', '2024-01-12', 'Jane Smith', 'High', '2024-01-12 09:15', NOW(), NOW()),
('MOP-2024-005', 'Fresh Foods Enterprise', 'Sole Proprietorship', '202101023456', 'Food & Beverage', '555 Food Street, Shah Alam, Malaysia', 'Tan Ah Kow', '750620-08-2345', '+60167890123', 'tan@freshfoods.com', 'Rejected', '2024-01-11', 'Mike Johnson', 'Normal', '2024-01-11 14:45', NOW(), NOW());

-- Insert Case History
INSERT INTO case_history (time, action, case_id) VALUES
('2024-01-15 10:30', 'Case created by John Doe', 'MOP-2024-001'),
('2024-01-15 10:35', 'Assigned to Compliance Team', 'MOP-2024-001'),
('2024-01-15 14:20', 'Documents uploaded', 'MOP-2024-001'),
('2024-01-14 09:00', 'Case created by Jane Smith', 'MOP-2024-002'),
('2024-01-14 16:00', 'Sent for background verification', 'MOP-2024-002'),
('2024-01-13 08:00', 'Case created by Mike Johnson', 'MOP-2024-003'),
('2024-01-13 11:30', 'Case approved', 'MOP-2024-003');

-- Insert Documents
INSERT INTO documents (name, type, uploaded_at, file_path, case_id) VALUES
('Business Registration Certificate', 'pdf', '2024-01-15 10:30', '/uploads/MOP-2024-001/registration.pdf', 'MOP-2024-001'),
('Director ID Copy', 'pdf', '2024-01-15 10:32', '/uploads/MOP-2024-001/director_id.pdf', 'MOP-2024-001'),
('Financial Statement', 'pdf', '2024-01-15 14:20', '/uploads/MOP-2024-001/financial.pdf', 'MOP-2024-001'),
('Business Registration Certificate', 'pdf', '2024-01-14 09:05', '/uploads/MOP-2024-002/registration.pdf', 'MOP-2024-002'),
('Director ID Copy', 'pdf', '2024-01-14 09:07', '/uploads/MOP-2024-002/director_id.pdf', 'MOP-2024-002');
