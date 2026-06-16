package com.civicdesk.module.publicworks.controller;

import com.civicdesk.module.publicworks.dto.request.*;
import com.civicdesk.module.publicworks.dto.response.*;
import com.civicdesk.module.publicworks.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkOrderControllerTest {

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private WorkOrderController workOrderController;

    // ── createWorkOrder ───────────────────────────────────────────────────

    @Test
    public void createWorkOrder_Success_Returns201() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setProjectName("Anna Nagar Pothole Repair");
        req.setCategory("RoadRepair");
        req.setWard("Ward 5");
        req.setZone("North");
        req.setBudgetAllocated(new BigDecimal("2500000.00"));
        req.setStartDate(LocalDate.of(2025, 2, 1));
        req.setExpectedEndDate(LocalDate.of(2025, 3, 15));

        doNothing().when(workOrderService).createWorkOrder(any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.createWorkOrder(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Work order created successfully", response.getBody().get("message"));
    }

    @Test
    public void createWorkOrder_Failure_Returns400() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setCategory("InvalidCategory");

        doThrow(new RuntimeException("Invalid category"))
                .when(workOrderService).createWorkOrder(any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.createWorkOrder(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create work order", response.getBody().get("message"));
    }

    // ── getAllWorkOrders ───────────────────────────────────────────────────

    @Test
    public void getAllWorkOrders_Success_Returns200() {
        WorkOrderSummaryResponse summary = new WorkOrderSummaryResponse();
        summary.setWorkOrderId("wo-00001-test");
        summary.setProjectName("Anna Nagar Pothole Repair");
        summary.setStatus("InProgress");

        when(workOrderService.getAllWorkOrders(any(), any(), any()))
                .thenReturn(List.of(summary));

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getAllWorkOrders(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work orders fetched successfully", response.getBody().get("message"));
        assertNotNull(response.getBody().get("workOrders"));
    }

    @Test
    public void getAllWorkOrders_Empty_ReturnsNoWorkOrdersMessage() {
        when(workOrderService.getAllWorkOrders(any(), any(), any()))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getAllWorkOrders(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No work orders found", response.getBody().get("message"));
    }

    // ── getWorkOrderById ──────────────────────────────────────────────────

    @Test
    public void getWorkOrderById_Success_Returns200() {
        WorkOrderDetailResponse detail = new WorkOrderDetailResponse();
        detail.setWorkOrderId("wo-00001-test");
        detail.setProjectName("Anna Nagar Pothole Repair");
        detail.setCategory("RoadRepair");
        detail.setWard("Ward 5");
        detail.setStatus("InProgress");
        detail.setBudgetAllocated(new BigDecimal("2500000.00"));
        detail.setBudgetConsumedTotal(BigDecimal.ZERO);

        when(workOrderService.getWorkOrderById("wo-00001-test")).thenReturn(detail);

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getWorkOrderById("wo-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order details fetched successfully", response.getBody().get("message"));
        assertEquals("wo-00001-test", response.getBody().get("workOrderId"));
    }

    @Test
    public void getWorkOrderById_NotFound_Returns404() {
        doThrow(new RuntimeException("not found"))
                .when(workOrderService).getWorkOrderById("wrong-id");

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getWorkOrderById("wrong-id");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Work order not found", response.getBody().get("message"));
    }

    // ── updateWorkOrder ───────────────────────────────────────────────────

    @Test
    public void updateWorkOrder_Success_Returns200() {
        UpdateWorkOrderRequest req = new UpdateWorkOrderRequest();
        req.setProjectName("Anna Nagar Pothole Repair — Revised");
        req.setBudgetAllocated(new BigDecimal("2700000.00"));

        doNothing().when(workOrderService).updateWorkOrder(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.updateWorkOrder("wo-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order updated successfully", response.getBody().get("message"));
    }

    @Test
    public void updateWorkOrder_Failure_Returns400() {
        UpdateWorkOrderRequest req = new UpdateWorkOrderRequest();
        doThrow(new RuntimeException("bad status")).when(workOrderService)
                .updateWorkOrder(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.updateWorkOrder("wo-00001-test", req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to update work order", response.getBody().get("message"));
    }

    // ── updateWorkOrderStatus ─────────────────────────────────────────────

    @Test
    public void updateWorkOrderStatus_Success_Returns200() {
        UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
        req.setStatus("InProgress");
        req.setRemarks("Contractor mobilised on site");

        doNothing().when(workOrderService).updateWorkOrderStatus(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.updateWorkOrderStatus("wo-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order status updated successfully", response.getBody().get("message"));
    }

    // ── assignContractor ──────────────────────────────────────────────────

    @Test
    public void assignContractor_Success_Returns200() {
        AssignContractorRequest req = new AssignContractorRequest();
        req.setAssignedContractorId("user-0009-0000-0000-000000000009");

        doNothing().when(workOrderService).assignContractor(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.assignContractor("wo-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Contractor assigned successfully", response.getBody().get("message"));
    }

    // ── completeWorkOrder ─────────────────────────────────────────────────

    @Test
    public void completeWorkOrder_Success_Returns200() {
        CompleteWorkOrderRequest req = new CompleteWorkOrderRequest();
        req.setActualEndDate(LocalDate.of(2025, 3, 10));
        req.setRemarks("All potholes resurfaced and verified.");

        doNothing().when(workOrderService).completeWorkOrder(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.completeWorkOrder("wo-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order marked as completed successfully", response.getBody().get("message"));
    }

    // ── cancelWorkOrder ───────────────────────────────────────────────────

    @Test
    public void cancelWorkOrder_Success_Returns200() {
        doNothing().when(workOrderService).cancelWorkOrder(anyString());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.cancelWorkOrder("wo-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order cancelled successfully", response.getBody().get("message"));
    }

    // ── createMilestone ───────────────────────────────────────────────────

    @Test
    public void createMilestone_Success_Returns201() {
        CreateMilestoneRequest req = new CreateMilestoneRequest();
        req.setDescription("Site survey and pothole marking complete");
        req.setPlannedDate(LocalDate.of(2025, 2, 7));

        doNothing().when(workOrderService).createMilestone(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.createMilestone("wo-00001-test", req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Milestone created successfully", response.getBody().get("message"));
    }

    @Test
    public void createMilestone_Failure_Returns400() {
        CreateMilestoneRequest req = new CreateMilestoneRequest();
        doThrow(new RuntimeException("error")).when(workOrderService)
                .createMilestone(anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.createMilestone("wrong-id", req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create milestone", response.getBody().get("message"));
    }

    // ── getMilestonesByWorkOrder ──────────────────────────────────────────

    @Test
    public void getMilestonesByWorkOrder_Success_Returns200() {
        MilestoneResponse ms = new MilestoneResponse();
        ms.setMilestoneId("ms-00001-test");
        ms.setStatus("Pending");

        when(workOrderService.getMilestonesByWorkOrder("wo-00001-test"))
                .thenReturn(List.of(ms));

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getMilestonesByWorkOrder("wo-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestones fetched successfully", response.getBody().get("message"));
        assertNotNull(response.getBody().get("milestones"));
    }

    @Test
    public void getMilestonesByWorkOrder_Empty_ReturnsNoMilestonesMessage() {
        when(workOrderService.getMilestonesByWorkOrder(anyString()))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getMilestonesByWorkOrder("wo-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No milestones found for this work order", response.getBody().get("message"));
    }

    // ── getMilestoneById ──────────────────────────────────────────────────

    @Test
    public void getMilestoneById_Success_Returns200() {
        MilestoneResponse ms = new MilestoneResponse();
        ms.setMilestoneId("ms-00001-test");
        ms.setWorkOrderId("wo-00001-test");
        ms.setDescription("Site survey");
        ms.setStatus("Pending");
        ms.setBudgetConsumed(BigDecimal.ZERO);

        when(workOrderService.getMilestoneById("wo-00001-test", "ms-00001-test"))
                .thenReturn(ms);

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getMilestoneById("wo-00001-test", "ms-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestone details fetched successfully", response.getBody().get("message"));
    }

    // ── updateMilestone ───────────────────────────────────────────────────

    @Test
    public void updateMilestone_Success_Returns200() {
        UpdateMilestoneRequest req = new UpdateMilestoneRequest();
        req.setDescription("Site survey — Ward 5 full coverage");

        doNothing().when(workOrderService).updateMilestone(anyString(), anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.updateMilestone("wo-00001-test", "ms-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestone updated successfully", response.getBody().get("message"));
    }

    // ── completeMilestone ─────────────────────────────────────────────────

    @Test
    public void completeMilestone_Success_Returns200() {
        CompleteMilestoneRequest req = new CompleteMilestoneRequest();
        req.setCompletedDate(LocalDate.of(2025, 2, 6));
        req.setBudgetConsumed(new BigDecimal("85000.00"));

        doNothing().when(workOrderService).completeMilestone(anyString(), anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.completeMilestone("wo-00001-test", "ms-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestone marked as completed successfully", response.getBody().get("message"));
    }

    // ── updateMilestoneStatus ─────────────────────────────────────────────

    @Test
    public void updateMilestoneStatus_Success_Returns200() {
        UpdateMilestoneStatusRequest req = new UpdateMilestoneStatusRequest();
        req.setStatus("Delayed");
        req.setRemarks("LED fixtures delayed due to supply chain issue.");

        doNothing().when(workOrderService).updateMilestoneStatus(anyString(), anyString(), any());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.updateMilestoneStatus("wo-00001-test", "ms-00001-test", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestone status updated successfully", response.getBody().get("message"));
    }

    // ── deleteMilestone ───────────────────────────────────────────────────

    @Test
    public void deleteMilestone_Success_Returns200() {
        doNothing().when(workOrderService).deleteMilestone(anyString(), anyString());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.deleteMilestone("wo-00001-test", "ms-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Milestone deleted successfully", response.getBody().get("message"));
    }

    // ── getBudgetSummary ──────────────────────────────────────────────────

    @Test
    public void getBudgetSummary_Success_Returns200() {
        BudgetSummaryResponse summary = new BudgetSummaryResponse();
        summary.setWorkOrderId("wo-00001-test");
        summary.setProjectName("Anna Nagar Pothole Repair");
        summary.setWard("Ward 5");
        summary.setBudgetAllocated(new BigDecimal("2500000.00"));
        summary.setBudgetConsumedTotal(new BigDecimal("85000.00"));
        summary.setUtilizationPct(3.40);
        summary.setStatus("InProgress");

        when(workOrderService.getBudgetSummary()).thenReturn(List.of(summary));

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getBudgetSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Budget summary fetched successfully", response.getBody().get("message"));
    }

    @Test
    public void getBudgetSummary_Empty_ReturnsNoDataMessage() {
        when(workOrderService.getBudgetSummary()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getBudgetSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No data available", response.getBody().get("message"));
    }

    // ── getDelayedMilestones ──────────────────────────────────────────────

    @Test
    public void getDelayedMilestones_Success_Returns200() {
        DelayedMilestoneResponse delayed = new DelayedMilestoneResponse();
        delayed.setMilestoneId("ms-00006-test");
        delayed.setStatus("Delayed");
        delayed.setDaysOverdue(12);

        when(workOrderService.getDelayedMilestones()).thenReturn(List.of(delayed));

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getDelayedMilestones();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Delayed milestones fetched successfully", response.getBody().get("message"));
    }

    @Test
    public void getDelayedMilestones_Empty_ReturnsNoDelayedMessage() {
        when(workOrderService.getDelayedMilestones()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getDelayedMilestones();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No delayed milestones found", response.getBody().get("message"));
    }

    // ── getPublicWorkOrdersByWard ──────────────────────────────────────────

    @Test
    public void getPublicWorkOrdersByWard_Success_Returns200() {
        PublicWorkOrderResponse pub = new PublicWorkOrderResponse();
        pub.setWorkOrderId("wo-00001-test");
        pub.setProjectName("Anna Nagar Pothole Repair");
        pub.setStatus("Completed");

        when(workOrderService.getPublicWorkOrdersByWard("Ward 5"))
                .thenReturn(List.of(pub));

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getPublicWorkOrdersByWard("Ward 5");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Public works orders fetched successfully", response.getBody().get("message"));
    }

    @Test
    public void getPublicWorkOrdersByWard_Empty_ReturnsNoActiveWorksMessage() {
        when(workOrderService.getPublicWorkOrdersByWard(anyString()))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getPublicWorkOrdersByWard("Ward 99");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No active works found for this ward", response.getBody().get("message"));
    }

    // ── getPublicWorkOrderDetail ──────────────────────────────────────────

    @Test
    public void getPublicWorkOrderDetail_Success_Returns200() {
        PublicWorkOrderResponse pub = new PublicWorkOrderResponse();
        pub.setWorkOrderId("wo-00001-test");
        pub.setProjectName("Anna Nagar Pothole Repair");
        pub.setStatus("Completed");
        pub.setTotalMilestones(2);
        pub.setCompletedMilestones(2);

        when(workOrderService.getPublicWorkOrderDetail("wo-00001-test")).thenReturn(pub);

        ResponseEntity<Map<String, Object>> response =
                workOrderController.getPublicWorkOrderDetail("wo-00001-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Work order details fetched successfully", response.getBody().get("message"));
        assertEquals(2, response.getBody().get("totalMilestones"));
    }
}
