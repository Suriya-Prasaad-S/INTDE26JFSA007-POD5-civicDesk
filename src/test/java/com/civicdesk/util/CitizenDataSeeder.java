package com.civicdesk.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

//Standalone data seeding utility for local performance testing.

public class CitizenDataSeeder {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/civicdesk?rewriteBatchedStatements=true";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root"; 


    private static final int TOTAL_RECORDS = 50_000;
    private static final int BATCH_SIZE = 5000;

    private static final String PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final String INSERT_SQL =
            "INSERT IGNORE INTO users " +
            "(userId, name, email, passwordHash, phone, role, departmentId, status, isPasswordSet, createdAt, updatedAt) " +
            "VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, NOW(), NOW())";

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Krishna",
            "Ishaan", "Rohan", "Ananya", "Diya", "Priya", "Aanya", "Aadhya", "Kavya",
            "Meera", "Riya", "Sara", "Anika", "Karthik", "Nikhil", "Rahul", "Varun",
            "Manish", "Pooja", "Sneha", "Divya", "Neha", "Shreya"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Patel", "Reddy", "Nair", "Iyer", "Menon", "Gupta",
            "Kumar", "Singh", "Rao", "Pillai", "Desai", "Joshi", "Chopra", "Malhotra",
            "Bose", "Das", "Banerjee", "Mehta", "Shah", "Kapoor", "Agarwal", "Pandey",
            "Mishra", "Tiwari", "Saxena", "Bhat", "Chowdhury", "Naidu"
    };

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try (Connection connection =
                     DriverManager.getConnection(JDBC_URL, DB_USERNAME, DB_PASSWORD)) {

            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {

                for (int i = 1; i <= TOTAL_RECORDS; i++) {
                    String firstName = FIRST_NAMES[(i - 1) % FIRST_NAMES.length];
                   
                    String lastName = LAST_NAMES[((i - 1) + (i / FIRST_NAMES.length)) % LAST_NAMES.length];

                    ps.setString(1, UUID.randomUUID().toString());      
                    ps.setString(2, "Citizen " + firstName + " " + lastName); 
                    ps.setString(3, "citizen" + i + "@perftest.com");   
                    ps.setString(4, PASSWORD_HASH);                     
                    ps.setString(5, "9" + String.format("%09d", i));   
                    ps.setString(6, "CIT");                             
                    ps.setString(7, "A");                               
                    ps.setBoolean(8, true);                             

                    ps.addBatch();

                    if (i % BATCH_SIZE == 0) {
                        ps.executeBatch();
                        connection.commit();
                        ps.clearBatch();
                        System.out.println("Inserted " + i + " / " + TOTAL_RECORDS);
                    }
                }

                int remainder = TOTAL_RECORDS % BATCH_SIZE;
                if (remainder != 0) {
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
        System.out.println("Done. " + TOTAL_RECORDS + " records inserted in " + elapsedMs + " ms");
    }
}