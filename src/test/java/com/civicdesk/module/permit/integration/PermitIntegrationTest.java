package com.civicdesk.module.permit.integration;

import com.civicdesk.module.permit.entity.CitizenProfile;
import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.entity.User;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import com.civicdesk.module.permit.repository.CitizenProfileRepository;
import com.civicdesk.module.permit.repository.PermitApplicationRepository;
import com.civicdesk.module.permit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
public class PermitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PermitApplicationRepository permitRepo;

    @Autowired
    private CitizenProfileRepository citizenRepo;

    @Autowired
    private UserRepository userRepo;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/civicDesk/permits";

        permitRepo.deleteAll();
        citizenRepo.deleteAll();
        userRepo.deleteAll();

        // Insert test citizen
        CitizenProfile citizen = new CitizenProfile();
        citizen.setCitizenId("cit-integration-001");
        citizen.setUserId("user-integration-001");
        citizen.setGender("MALE");
        citizen.setNationalId("AADHAAR-TEST-001");
        citizen.setAddress("12 Anna Nagar, Chennai");
        citizen.setWard("Ward 5");
        citizen.setZone("North");
        citizen.setProfileStatus("Verified");
        citizenRepo.save(citizen);

        // Insert test user
        User user = new User();
        user.setUserId("user-integration-001");
        user.setName("Test Citizen");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setPhone("9876543210");
        user.setRole("CITIZEN");
        user.setStatus("Active");
        userRepo.save(user);
    }

    @Test
    public void createPermit_Integration_Success() {
        // Arrange
        String requestBody = "{"
                + "\"permitType\": \"BuildingPermit\","
                + "\"propertyAddress\": \"12 Anna Nagar, Chennai\","
                + "\"ward\": \"Ward 5\","
                + "\"zone\": \"North\","
                + "\"validityPeriod\": 24,"
                + "\"fee\": 15000.00"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Citizen-Id", "cit-integration-001");

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/createPermit",
                HttpMethod.POST,
                request,
                Map.class);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Permit application created successfully",
                response.getBody().get("message"));
    }

    @Test
    public void createPermit_Integration_CitizenNotFound_Failure() {
        // Arrange
        String requestBody = "{"
                + "\"permitType\": \"BuildingPermit\","
                + "\"propertyAddress\": \"12 Anna Nagar, Chennai\","
                + "\"ward\": \"Ward 5\","
                + "\"zone\": \"North\""
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Citizen-Id", "wrong-citizen-id");

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/createPermit",
                HttpMethod.POST,
                request,
                Map.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create permit application",
                response.getBody().get("message"));
    }

    @Test
    public void getAllPermits_Integration_EmptyList() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Citizen-Id", "cit-integration-001");
        HttpEntity<String> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/getAllPermits",
                HttpMethod.GET,
                request,
                Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No permits found", response.getBody().get("message"));
    }

    @Test
    public void getPermitDetail_Integration_NotFound() {
        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wrong-permit-id",
                HttpMethod.GET,
                null,
                Map.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Permit not found", response.getBody().get("message"));
    }

    @Test
    public void getQueue_Integration_Success() {
        // Arrange
        PermitApplication permit = new PermitApplication();
        permit.setPermitId("perm-integration-001");
        permit.setCitizenId("cit-integration-001");
        permit.setPermitType(PermitType.BuildingPermit);
        permit.setApplicationDate(LocalDate.now());
        permit.setPropertyAddress("12 Anna Nagar, Chennai");
        permit.setWard("Ward 5");
        permit.setZone("North");
        permit.setStatus(PermitStatus.Applied);
        permit.setDeleted(false);
        permit.setCreatedAt(LocalDateTime.now());
        permit.setUpdatedAt(LocalDateTime.now());
        permitRepo.save(permit);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/queue",
                HttpMethod.GET,
                null,
                Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Application queue fetched successfully",
                response.getBody().get("message"));
        assertNotNull(response.getBody().get("applications"));
    }
}