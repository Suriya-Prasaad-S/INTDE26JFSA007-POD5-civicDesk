-- =============================================================================
-- CivicDesk IAM — MySQL 8 schema
-- Generated from the JPA entities:
--   com.civicdesk.module.iam.entity      — Department.java, User.java
--   com.civicdesk.module.auditlog.entity — AuditLog.java
--
-- Notes:
--  * Column names are camelCase: the app uses PhysicalNamingStrategyStandardImpl,
--    so Hibernate does NOT snake_case identifiers.
--  * The entities map departmentId / userId as plain String columns (no @ManyToOne),
--    so Hibernate's ddl-auto=update does not create foreign keys. The FK block at
--    the bottom is OPTIONAL and recommended for a hand-managed database.
--  * status is a single-char code: 'A' (Active), 'I' (Inactive), 'S' (Suspended).
--  * role is one of: ADM, DS, FO, ENG, CO, CIT.
-- =============================================================================

-- CREATE DATABASE IF NOT EXISTS civicdesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE civicdesk;

-- -----------------------------------------------------------------------------
-- departments  (entity: Department)
--   departmentId is a human-readable business key: DPT01, DPT02, … (length 10)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
  departmentId            VARCHAR(10)  NOT NULL,
  name                    VARCHAR(100) NOT NULL,
  departmentSupervisorId  VARCHAR(20)  NULL,
  CONSTRAINT pk_departments     PRIMARY KEY (departmentId),
  CONSTRAINT uq_department_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- users  (entity: User)
--   userId is a sequential numeric string: 10000001, 10000002, …
--   passwordHash is NULL until the account owner sets a password.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  userId         VARCHAR(20)  NOT NULL,
  name           VARCHAR(100) NOT NULL,
  email          VARCHAR(150) NOT NULL,
  passwordHash   VARCHAR(255) NULL,
  isPasswordSet  BIT(1)       NOT NULL DEFAULT b'0',
  phone          VARCHAR(15)  NULL,
  role           VARCHAR(30)  NOT NULL,
  departmentId   VARCHAR(10)  NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'A',
  createdAt      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updatedAt      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_users      PRIMARY KEY (userId),
  CONSTRAINT uq_user_email UNIQUE (email),
  INDEX idx_users_role        (role),
  INDEX idx_users_status      (status),
  INDEX idx_users_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- audit_log  (entity: AuditLog)
--   auditId is a sequential numeric string: 10000001, 10000002, …
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
  auditId    VARCHAR(20) NOT NULL,
  userId     VARCHAR(20) NOT NULL,
  action     VARCHAR(50) NOT NULL,
  module     VARCHAR(50) NOT NULL,
  ipAddress  VARCHAR(45) NULL,
  timestamp  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_log PRIMARY KEY (auditId),
  INDEX idx_audit_userId    (userId),
  INDEX idx_audit_action    (action),
  INDEX idx_audit_module    (module),
  INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- OPTIONAL foreign keys (not created by Hibernate; add them for a managed DB).
-- users.departmentId references a department; departments.departmentSupervisorId
-- references the heading user — defined after both tables exist to avoid the cycle.
-- -----------------------------------------------------------------------------
-- ALTER TABLE users
--   ADD CONSTRAINT fk_user_department FOREIGN KEY (departmentId)
--   REFERENCES departments(departmentId)
--   ON DELETE SET NULL ON UPDATE CASCADE;

-- ALTER TABLE departments
--   ADD CONSTRAINT fk_dept_supervisor FOREIGN KEY (departmentSupervisorId)
--   REFERENCES users(userId)
--   ON DELETE SET NULL ON UPDATE CASCADE;

-- ALTER TABLE audit_log
--   ADD CONSTRAINT fk_audit_user FOREIGN KEY (userId)
--   REFERENCES users(userId)
--   ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================================================
-- Seed data
-- The application's DataSeeder seeds the six departments and the first ADM
-- account automatically on startup, so the rows below are OPTIONAL — useful only
-- for a database you stand up by hand. The passwordHash is a bcrypt hash of the
-- default admin password (app.admin.password = 'Admin@12345').
-- =============================================================================
INSERT INTO departments (departmentId, name, departmentSupervisorId) VALUES
  ('DPT01', 'Infrastructure',         NULL),
  ('DPT02', 'Public Health',          NULL),
  ('DPT03', 'Licensing & Compliance', NULL),
  ('DPT04', 'Citizen Services',       NULL),
  ('DPT05', 'Administration',         NULL),
  ('DPT06', 'Compliance & Audit',     NULL);

INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000001', 'System Administrator', 'admin@civicdesk.gov',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', b'1', '9999999999', 'ADM', NULL, 'A');

INSERT INTO audit_log (auditId, userId, action, module, ipAddress) VALUES
  ('10000001', '10000001', 'SEED_ADMIN', 'IAM', 'system');

-- =============================================================================
-- Sample DML — 5 INSERT / 5 UPDATE / 5 SELECT / 5 DROP
-- Demonstration statements only; the application manages real data at runtime.
-- =============================================================================

-- ---- 5 INSERT ---------------------------------------------------------------
-- Staff accounts (passwordHash NULL + isPasswordSet 0: owner sets password on first login).
INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000002', 'Meena Rao',  'meena@civicdesk.gov', NULL, b'0', '9000000002', 'DS',  'DPT01', 'A');
INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000003', 'Arjun Das',  'arjun@civicdesk.gov', NULL, b'0', '9000000003', 'DS',  'DPT02', 'A');
INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000004', 'Priya Nair', 'priya@civicdesk.gov', NULL, b'0', '9000000004', 'FO',  'DPT01', 'A');
INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000005', 'Karan Iyer', 'karan@civicdesk.gov', NULL, b'0', '9000000005', 'ENG', 'DPT01', 'A');
-- Citizen account.
INSERT INTO users (userId, name, email, passwordHash, isPasswordSet, phone, role, departmentId, status) VALUES
  ('10000006', 'Ravi Kumar', 'ravi@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', b'1', '9876543210', 'CIT', NULL, 'A');

-- ---- 5 UPDATE ---------------------------------------------------------------
-- Assign department supervisors (the DS who heads each department).
UPDATE departments SET departmentSupervisorId = '10000002' WHERE departmentId = 'DPT01';
UPDATE departments SET departmentSupervisorId = '10000003' WHERE departmentId = 'DPT02';
-- Suspend a user account.
UPDATE users SET status = 'S' WHERE userId = '10000006';
-- Move a field officer to another department.
UPDATE users SET departmentId = 'DPT02' WHERE userId = '10000004';
-- Mark a password as set for a staff member.
UPDATE users
   SET passwordHash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       isPasswordSet = b'1'
 WHERE userId = '10000002';

-- ---- 5 SELECT ---------------------------------------------------------------
SELECT * FROM departments;
SELECT userId, name, email, role, status FROM users WHERE status = 'A';
SELECT role, COUNT(*) AS total FROM users GROUP BY role;
SELECT u.userId, u.name, u.role, d.name AS department
  FROM users u
  LEFT JOIN departments d ON u.departmentId = d.departmentId;
SELECT * FROM audit_log ORDER BY timestamp DESC;

-- ---- 5 DROP -----------------------------------------------------------------
-- Drop indexes first (illustrative), then the tables. FK checks are toggled off
-- so the tables can be dropped regardless of declaration order.
SET FOREIGN_KEY_CHECKS = 0;
DROP INDEX idx_users_role_status ON users;
DROP INDEX idx_audit_timestamp ON audit_log;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;
SET FOREIGN_KEY_CHECKS = 1;
