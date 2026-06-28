package com.civicdesk.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

// Standalone data-seeding utility for local performance testing of the audit_log table.
// Run main() against a local/test database to bulk-load audit entries, then exercise the
// indexed GET /audit/auditLogs filters (userId / action / module). Mirrors CitizenDataSeeder.
//
// auditId is the varchar primary key (app-generated ids are sequential numbers starting
// at 10000001); to avoid colliding with those, seeded rows start from BASE_AUDIT_ID.
// userId is drawn from a pool that overlaps the seeded citizen ids so userId filters match.

public class AuditLogDataSeeder {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/civicdesk?rewriteBatchedStatements=true";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";

    private static final int TOTAL_RECORDS = 100_000;
    private static final int BATCH_SIZE = 5000;

    /** Seeded auditIds run from here upward, well clear of the app sequence (starts 10000001). */
    private static final long BASE_AUDIT_ID = 900_000_000L;

    /** userIds are picked from [FIRST_USER_ID, FIRST_USER_ID + USER_POOL) so filters return rows. */
    private static final long FIRST_USER_ID = 10_000_001L;
    private static final int USER_POOL = 50_000;

    // Hard-coded to keep this utility free of app dependencies (matches CitizenDataSeeder's style).
    // Keep in sync with enums AuditAction and AuditModule.
    private static final String[] ACTIONS = {
            "REGISTER", "LOGIN", "LOGOUT", "CREATE_USER", "UPDATE_STATUS", "SET_PASSWORD", "SEED_ADMIN"
    };
    private static final String[] MODULES = {
            "IAM", "SERVICE_REQUEST", "GRIEVANCE", "PERMIT", "WORKS"
    };

    private static final String INSERT_SQL =
            "INSERT IGNORE INTO audit_log " +
            "(auditId, userId, action, module, ipAddress, timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try (Connection connection =
                     DriverManager.getConnection(JDBC_URL, DB_USERNAME, DB_PASSWORD)) {

            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {

                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                LocalDateTime now = LocalDateTime.now();

                for (int i = 1; i <= TOTAL_RECORDS; i++) {
                    long userId = FIRST_USER_ID + rnd.nextInt(USER_POOL);

                    ps.setString(1, String.valueOf(BASE_AUDIT_ID + i));            // auditId
                    ps.setString(2, String.valueOf(userId));                       // userId
                    ps.setString(3, ACTIONS[rnd.nextInt(ACTIONS.length)]);         // action
                    ps.setString(4, MODULES[rnd.nextInt(MODULES.length)]);         // module
                    ps.setString(5, "10." + rnd.nextInt(256) + "."                 // ipAddress
                            + rnd.nextInt(256) + "." + rnd.nextInt(256));
                    // Spread timestamps over the last ~90 days for realistic time-ordered paging.
                    ps.setTimestamp(6, Timestamp.valueOf(now.minusMinutes(rnd.nextLong(129_600))));

                    ps.addBatch();

                    if (i % BATCH_SIZE == 0) {
                        ps.executeBatch();
                        connection.commit();
                        ps.clearBatch();
                        System.out.println("Inserted " + i + " / " + TOTAL_RECORDS);
                    }
                }

                if (TOTAL_RECORDS % BATCH_SIZE != 0) {
                    ps.executeBatch();
                    connection.commit();
                    ps.clearBatch();
                    System.out.println("Inserted " + TOTAL_RECORDS + " / " + TOTAL_RECORDS);
                }
            }

        } catch (SQLException e) {
            System.err.println("Seeding failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        System.out.println("Done. " + TOTAL_RECORDS + " audit records inserted in " + elapsedMs + " ms");
    }
}
