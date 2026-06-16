-- ============================================================================
--  MOCK reference schema for local Module 2.2 (citizen) testing — NOT my tables.
--  `users` and `departments` are owned by Module 2.1 (IAM), which hasn't built its
--  entities yet. The citizen module only references them by value (CHAR(36) ids:
--  CitizenDocument.verifiedBy -> users.userId), so these tables let Postman tests use
--  real user ids. Created only under the `local` Spring profile (see
--  application-local.properties). IF NOT EXISTS keeps it safe if IAM later provides them.
-- ============================================================================

CREATE TABLE IF NOT EXISTS departments (
  departmentId           CHAR(36)     NOT NULL,
  departmentName         VARCHAR(150) NOT NULL,
  departmentSupervisorId CHAR(36)     NULL,
  email                  VARCHAR(150) NOT NULL,
  CONSTRAINT pk_departments      PRIMARY KEY (departmentId),
  CONSTRAINT uq_department_email UNIQUE      (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  userId       CHAR(36)     NOT NULL,
  name         VARCHAR(100) NOT NULL,
  email        VARCHAR(150) NOT NULL,
  passwordHash VARCHAR(255) NOT NULL,
  phone        VARCHAR(15)  NULL,
  role         VARCHAR(30)  NOT NULL,
  departmentId CHAR(36)     NULL,
  status       VARCHAR(20)  NOT NULL DEFAULT 'Active',
  createdAt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updatedAt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_users           PRIMARY KEY (userId),
  CONSTRAINT uq_user_email      UNIQUE      (email),
  CONSTRAINT chk_user_role      CHECK       (role   IN ('ADMIN','DEPT_SUPERVISOR','FIELD_OFFICER','ENGINEER','COMPLIANCE_OFFICER','CITIZEN')),
  CONSTRAINT chk_user_status    CHECK       (status IN ('Active','Inactive','Suspended')),
  CONSTRAINT fk_user_department FOREIGN KEY (departmentId)
    REFERENCES departments(departmentId)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
