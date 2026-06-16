package com.civicdesk.module.publicworks.integration;

import com.civicdesk.module.publicworks.entity.Milestone;
import com.civicdesk.module.publicworks.entity.WorkOrder;
import com.civicdesk.module.publicworks.repository.MilestoneRepository;
import com.civicdesk.module.publicworks.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_pw;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
public class PublicWorksIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WorkOrderRepository workOrderRepo;

    @Autowired
    private MilestoneRepository milestoneRepo;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/civicDesk/workorders";
        milestoneRepo.deleteAll();
        workOrderRepo.deleteAll();
    }

    // ── createWorkOrder integration ───────────────────────────────────────

    @Test
    public void createWorkOrder_Integration_Success() {
        String body = "{"
                + "\"projectName\": \"Anna Nagar Pothole Repair Phase 1\","
                + "\"category\": \"RoadRepair\","
                + "\"ward\": \"Ward 5\","
                + "\"zone\": \"North\","
                + "\"budgetAllocated\": 2500000.00,"
                + "\"startDate\": \"2025-02-01\","
                + "\"expectedEndDate\": \"2025-03-15\","
                + "\"remarks\": \"Priority pothole repair\""
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/createWorkOrder", HttpMethod.POST, request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Work order created successfully", response.getBody().get("message"));
    }

    @Test
    public void createWorkOrder_Integration_InvalidCategory_Returns400() {
        String body = "{"
                + "\"projectName\": \"Test Project\","
                + "\"category\": \"InvalidCategory\","
                + "\"ward\": \"Ward 5\","
                + "\"budgetAllocated\": 1000000.00,"
                + "\"startDate\": \"2025-02-01\","
                + "\"expectedEndDate\": \"2025-03-15\""
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/createWorkOrder", HttpMethod.POST, request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create work order", response.getBody().get("message"));
    }

    // ── getAllWorkOrders integration ───────────────────────────────────────

    @Test
    public void getAllWorkOrders_Integration_ReturnsWorkOrders() {
        saveTestWorkOrder("wo-int-001", "Ward 5", "RoadRepair", "Planned");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/getAllWorkOrders", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work orders fetched successfully", response.getBody().get("message"));
        assertNotNull(response.getBody().get("workOrders"));
    }

    @Test
    public void getAllWorkOrders_Integration_EmptyList() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/getAllWorkOrders", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No work orders found", response.getBody().get("message"));
    }

    // ── getWorkOrderById integration ──────────────────────────────────────

    @Test
    public void getWorkOrderById_Integration_Success() {
        saveTestWorkOrder("wo-int-002", "Ward 5", "RoadRepair", "Planned");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wo-int-002", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order details fetched successfully",
                response.getBody().get("message"));
        assertEquals("wo-int-002", response.getBody().get("workOrderId"));
    }

    @Test
    public void getWorkOrderById_Integration_NotFound() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wrong-id", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Work order not found", response.getBody().get("message"));
    }

    // ── updateWorkOrderStatus integration ─────────────────────────────────

    @Test
    public void updateWorkOrderStatus_Integration_Success() {
        saveTestWorkOrder("wo-int-003", "Ward 5", "RoadRepair", "Planned");

        String body = "{\"status\": \"InProgress\", \"remarks\": \"Contractor mobilised\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wo-int-003/updateStatus", HttpMethod.PUT, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order status updated successfully",
                response.getBody().get("message"));
    }

    // ── milestone integration ─────────────────────────────────────────────

    @Test
    public void createMilestone_Integration_Success() {
        saveTestWorkOrder("wo-int-004", "Ward 5", "Drainage", "Planned");

        String body = "{"
                + "\"description\": \"Site survey and pothole marking complete\","
                + "\"plannedDate\": \"2025-02-07\""
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wo-int-004/milestones/createMilestone",
                HttpMethod.POST, request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Milestone created successfully", response.getBody().get("message"));
    }

    @Test
    public void getMilestones_Integration_EmptyList() {
        saveTestWorkOrder("wo-int-005", "Ward 8", "Lighting", "InProgress");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/wo-int-005/milestones", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No milestones found for this work order",
                response.getBody().get("message"));
    }

    // ── public endpoints integration ──────────────────────────────────────

    @Test
    public void getPublicWorkOrdersByWard_Integration_Success() {
        saveTestWorkOrder("wo-int-006", "Ward 5", "ParkMaintenance", "Planned");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/public/getByWard?ward=Ward 5",
                HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Public works orders fetched successfully",
                response.getBody().get("message"));
    }

    @Test
    public void getPublicWorkOrdersByWard_Integration_EmptyWard() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/public/getByWard?ward=Ward 99",
                HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No active works found for this ward",
                response.getBody().get("message"));
    }

    @Test
    public void getBudgetSummary_Integration_Success() {
        saveTestWorkOrder("wo-int-007", "Ward 12", "Drainage", "InProgress");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/budgetSummary", HttpMethod.GET, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Budget summary fetched successfully", response.getBody().get("message"));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void saveTestWorkOrder(String id, String ward, String category, String status) {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderId(id);
        wo.setProjectName("Test Project " + id);
        wo.setCategory(category);
        wo.setWard(ward);
        wo.setZone("North");
        wo.setBudgetAllocated(new BigDecimal("1000000.00"));
        wo.setBudgetConsumedTotal(BigDecimal.ZERO);
        wo.setStartDate(LocalDate.of(2025, 2, 1));
        wo.setExpectedEndDate(LocalDate.of(2025, 5, 1));
        wo.setStatus(status);
        wo.setDeleted(false);
        wo.setCreatedAt(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        workOrderRepo.save(wo);
    }
}
