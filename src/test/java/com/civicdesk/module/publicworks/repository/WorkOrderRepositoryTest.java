package com.civicdesk.module.publicworks.repository;

import com.civicdesk.module.publicworks.entity.Milestone;
import com.civicdesk.module.publicworks.entity.WorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
})
public class WorkOrderRepositoryTest {

    @Autowired
    private WorkOrderRepository workOrderRepo;

    @Autowired
    private MilestoneRepository milestoneRepo;

    private WorkOrder testWorkOrder;

    @BeforeEach
    public void setUp() {
        milestoneRepo.deleteAll();
        workOrderRepo.deleteAll();

        testWorkOrder = new WorkOrder();
        testWorkOrder.setWorkOrderId("wo-test-repo-001");
        testWorkOrder.setProjectName("Anna Nagar Pothole Repair Phase 1");
        testWorkOrder.setCategory("RoadRepair");
        testWorkOrder.setWard("Ward 5");
        testWorkOrder.setZone("North");
        testWorkOrder.setBudgetAllocated(new BigDecimal("2500000.00"));
        testWorkOrder.setBudgetConsumedTotal(BigDecimal.ZERO);
        testWorkOrder.setStartDate(LocalDate.of(2025, 2, 1));
        testWorkOrder.setExpectedEndDate(LocalDate.of(2025, 3, 15));
        testWorkOrder.setStatus("Planned");
        testWorkOrder.setDeleted(false);
        testWorkOrder.setCreatedAt(LocalDateTime.now());
        testWorkOrder.setUpdatedAt(LocalDateTime.now());

        workOrderRepo.save(testWorkOrder);
    }

    @Test
    public void findByWorkOrderIdAndIsDeletedFalse_Success() {
        Optional<WorkOrder> result =
                workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-test-repo-001");

        assertTrue(result.isPresent());
        assertEquals("wo-test-repo-001", result.get().getWorkOrderId());
    }

    @Test
    public void findByWorkOrderIdAndIsDeletedFalse_DeletedWorkOrder_ReturnsEmpty() {
        testWorkOrder.setDeleted(true);
        workOrderRepo.save(testWorkOrder);

        Optional<WorkOrder> result =
                workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-test-repo-001");

        assertFalse(result.isPresent());
    }

    @Test
    public void findByIsDeletedFalse_Success() {
        List<WorkOrder> result = workOrderRepo.findByIsDeletedFalse();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("wo-test-repo-001", result.get(0).getWorkOrderId());
    }

    @Test
    public void findByWardAndIsDeletedFalse_Success() {
        List<WorkOrder> result = workOrderRepo.findByWardAndIsDeletedFalse("Ward 5");

        assertFalse(result.isEmpty());
        assertEquals("Ward 5", result.get(0).getWard());
    }

    @Test
    public void findByWardAndIsDeletedFalse_WrongWard_ReturnsEmpty() {
        List<WorkOrder> result = workOrderRepo.findByWardAndIsDeletedFalse("Ward 99");

        assertTrue(result.isEmpty());
    }

    @Test
    public void findByCategoryAndIsDeletedFalse_Success() {
        List<WorkOrder> result =
                workOrderRepo.findByCategoryAndIsDeletedFalse("RoadRepair");

        assertFalse(result.isEmpty());
        assertEquals("RoadRepair", result.get(0).getCategory());
    }

    @Test
    public void findByStatusAndIsDeletedFalse_Success() {
        List<WorkOrder> result =
                workOrderRepo.findByStatusAndIsDeletedFalse("Planned");

        assertFalse(result.isEmpty());
        assertEquals("Planned", result.get(0).getStatus());
    }

    @Test
    public void findByWardAndCategoryAndIsDeletedFalse_Success() {
        List<WorkOrder> result =
                workOrderRepo.findByWardAndCategoryAndIsDeletedFalse("Ward 5", "RoadRepair");

        assertFalse(result.isEmpty());
    }

    @Test
    public void findByWardAndCategoryAndStatusAndIsDeletedFalse_Success() {
        List<WorkOrder> result =
                workOrderRepo.findByWardAndCategoryAndStatusAndIsDeletedFalse(
                        "Ward 5", "RoadRepair", "Planned");

        assertFalse(result.isEmpty());
    }

    // ── Milestone repository tests ────────────────────────────────────────

    @Test
    public void milestone_findByWorkOrderIdAndIsDeletedFalse_Success() {
        Milestone ms = new Milestone();
        ms.setMilestoneId("ms-repo-test-001");
        ms.setWorkOrderId("wo-test-repo-001");
        ms.setDescription("Site survey and pothole marking");
        ms.setPlannedDate(LocalDate.of(2025, 2, 7));
        ms.setBudgetConsumed(BigDecimal.ZERO);
        ms.setStatus("Pending");
        ms.setDeleted(false);
        ms.setCreatedAt(LocalDateTime.now());
        ms.setUpdatedAt(LocalDateTime.now());
        milestoneRepo.save(ms);

        List<Milestone> result =
                milestoneRepo.findByWorkOrderIdAndIsDeletedFalse("wo-test-repo-001");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("ms-repo-test-001", result.get(0).getMilestoneId());
    }

    @Test
    public void milestone_findByMilestoneIdAndIsDeletedFalse_Success() {
        Milestone ms = new Milestone();
        ms.setMilestoneId("ms-repo-test-002");
        ms.setWorkOrderId("wo-test-repo-001");
        ms.setDescription("Road patching phase 1");
        ms.setPlannedDate(LocalDate.of(2025, 3, 1));
        ms.setBudgetConsumed(BigDecimal.ZERO);
        ms.setStatus("Pending");
        ms.setDeleted(false);
        ms.setCreatedAt(LocalDateTime.now());
        ms.setUpdatedAt(LocalDateTime.now());
        milestoneRepo.save(ms);

        Optional<Milestone> result =
                milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-repo-test-002");

        assertTrue(result.isPresent());
        assertEquals("ms-repo-test-002", result.get().getMilestoneId());
    }

    @Test
    public void milestone_findByMilestoneIdAndIsDeletedFalse_Deleted_ReturnsEmpty() {
        Milestone ms = new Milestone();
        ms.setMilestoneId("ms-repo-test-003");
        ms.setWorkOrderId("wo-test-repo-001");
        ms.setDescription("Deleted milestone");
        ms.setPlannedDate(LocalDate.of(2025, 3, 1));
        ms.setBudgetConsumed(BigDecimal.ZERO);
        ms.setStatus("Pending");
        ms.setDeleted(true);
        ms.setCreatedAt(LocalDateTime.now());
        ms.setUpdatedAt(LocalDateTime.now());
        milestoneRepo.save(ms);

        Optional<Milestone> result =
                milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-repo-test-003");

        assertFalse(result.isPresent());
    }

    @Test
    public void milestone_findByStatusAndIsDeletedFalse_Success() {
        Milestone ms = new Milestone();
        ms.setMilestoneId("ms-repo-test-004");
        ms.setWorkOrderId("wo-test-repo-001");
        ms.setDescription("Delayed milestone");
        ms.setPlannedDate(LocalDate.of(2025, 2, 7));
        ms.setBudgetConsumed(BigDecimal.ZERO);
        ms.setStatus("Delayed");
        ms.setDeleted(false);
        ms.setCreatedAt(LocalDateTime.now());
        ms.setUpdatedAt(LocalDateTime.now());
        milestoneRepo.save(ms);

        List<Milestone> result = milestoneRepo.findByStatusAndIsDeletedFalse("Delayed");

        assertFalse(result.isEmpty());
        assertEquals("Delayed", result.get(0).getStatus());
    }
}
