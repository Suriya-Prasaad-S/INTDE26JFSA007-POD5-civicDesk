package com.civicdesk.module.grievance;

import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.config.JwtConfig;
import org.junit.jupiter.api.Test;

/**
 * DEV-ONLY helper — mints JWTs for manual API testing (Postman / Swagger).
 * NOT a real test; asserts nothing. Touches no IAM code; needs no DB or Spring context.
 *
 * <pre>
 * Run all tokens:   ./mvnw.cmd -Dtest=DevTokenMintTest test
 * Run just one:     ./mvnw.cmd -Dtest=DevTokenMintTest#printSupervisorToken test
 * </pre>
 * Each token is also written to target/dev-token-&lt;ROLE&gt;.txt (clean single line).
 * Delete this file before merging.
 */
class DevTokenMintTest {

    // Must match app.jwt.* in application.properties (or your env override).
    private static final String JWT_SECRET = "civicdesk_hs256_secret_key_minimum_32_characters_required";
    private static final long JWT_EXPIRY_MS = 1_800_000L; // 30 minutes

    // CITIZEN: a made-up id is fine — the grievance tables have no FK to users.
    private static final String CIT_USER_ID = "eea5c056-d612-494a-a2da-2a2caf2986a9";

    // ADMIN: the seeded ADM's userId (used for IAM admin endpoints).
    private static final String ADM_USER_ID = "195ce4c5-e787-4daa-8ddc-ee7b01181a7a";

    // SUPERVISOR: MUST be a REAL DS user's userId that HAS a departmentId — the supervisor
    // endpoints look the user up to find their department. Paste the DS user's id here.
    private static final String DS_USER_ID = "02fe10e0-0ca1-46f0-847c-5309081edbd0";

    @Test
    void printCitizenToken() {
        print("CIT", CIT_USER_ID, "CIT");
    }

    @Test
    void printAdminToken() {
        print("ADM", ADM_USER_ID, "ADM");
    }

    @Test
    void printSupervisorToken() {
        print("DS", DS_USER_ID, "DS");
    }

    private void print(String label, String userId, String role) {
        JwtConfig cfg = new JwtConfig();
        cfg.setSecret(JWT_SECRET);
        cfg.setExpiry(JWT_EXPIRY_MS);

        String bearer = "Bearer " + new JwtUtil(cfg).generateToken(userId, role);

        System.out.println("\n===== " + label + " TOKEN (valid 30 min) =====");
        System.out.println("userId = " + userId + ", role = " + role);
        System.out.println(bearer);
        System.out.println("====================================\n");

        // Also write a clean, single-line copy to target/dev-token-<label>.txt
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("target");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("dev-token-" + label + ".txt"), bearer);
        } catch (java.io.IOException e) {
            System.out.println("Could not write token file: " + e.getMessage());
        }
    }
}
