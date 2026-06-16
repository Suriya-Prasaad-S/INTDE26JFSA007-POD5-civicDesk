-- ============================================================================
--  MOCK seed data for local Module 2.2 (citizen) testing (IAM's users/departments).
--  Fixed, readable UUIDs so the Postman collection can reference known ids (e.g. a
--  verifier's userId for verifyDocument). INSERT IGNORE => idempotent across restarts
--  (this runs on every boot under the `local` profile). passwordHash is a throwaway
--  placeholder — security is disabled for Tier-2 testing.
-- ============================================================================

-- Departments (insert first: users.departmentId FKs to these) -----------------
INSERT IGNORE INTO departments (departmentId, departmentName, departmentSupervisorId, email) VALUES
  ('d1000000-0000-0000-0000-000000000001', 'Compliance & Audit', '20000000-0000-0000-0000-000000000002', 'compliance@civicdesk.gov'),
  ('d2000000-0000-0000-0000-000000000002', 'Public Works',       '70000000-0000-0000-0000-000000000007', 'publicworks@civicdesk.gov'),
  ('d3000000-0000-0000-0000-000000000003', 'Revenue & Tax',      NULL,                                   'revenue@civicdesk.gov');

-- Users (one per role; verifiers for verifyDocument are FIELD_OFFICER / COMPLIANCE_OFFICER) --
INSERT IGNORE INTO users (userId, name, email, passwordHash, phone, role, departmentId, status) VALUES
  ('10000000-0000-0000-0000-000000000001', 'System Admin',     'admin@civicdesk.gov',      '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000001', 'ADMIN',              NULL,                                   'Active'),
  ('20000000-0000-0000-0000-000000000002', 'Asha Menon',       'asha.menon@civicdesk.gov', '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000002', 'DEPT_SUPERVISOR',    'd1000000-0000-0000-0000-000000000001', 'Active'),
  ('30000000-0000-0000-0000-000000000003', 'Ravi Kumar',       'ravi.kumar@civicdesk.gov', '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000003', 'COMPLIANCE_OFFICER', 'd1000000-0000-0000-0000-000000000001', 'Active'),
  ('40000000-0000-0000-0000-000000000004', 'Neha Singh',       'neha.singh@civicdesk.gov', '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000004', 'FIELD_OFFICER',      'd2000000-0000-0000-0000-000000000002', 'Active'),
  ('50000000-0000-0000-0000-000000000005', 'Vikram Rao',       'vikram.rao@civicdesk.gov', '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000005', 'ENGINEER',           'd2000000-0000-0000-0000-000000000002', 'Active'),
  ('60000000-0000-0000-0000-000000000006', 'Meera Nair',       'meera.nair@civicdesk.gov', '$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000006', 'CITIZEN',            NULL,                                   'Active'),
  ('70000000-0000-0000-0000-000000000007', 'Sanjay Gupta',     'sanjay.gupta@civicdesk.gov','$2a$10$mockmockmockmockmockmuLb1n0Qd0m0c0k0m0c0k0m0c0k0m0c0', '9000000007', 'DEPT_SUPERVISOR',    'd2000000-0000-0000-0000-000000000002', 'Active');
