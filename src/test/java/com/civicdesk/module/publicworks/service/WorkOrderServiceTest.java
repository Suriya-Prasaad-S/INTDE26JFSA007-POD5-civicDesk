package com.civicdesk.module.publicworks.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.publicworks.dto.request.*;
import com.civicdesk.module.publicworks.dto.response.*;
import com.civicdesk.module.publicworks.entity.Milestone;
import com.civicdesk.module.publicworks.entity.WorkOrder;
import com.civicdesk.module.publicworks.repository.MilestoneRepository;
import com.civicdesk.module.publicworks.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepo;

    @Mock
    private MilestoneRepository milestoneRepo;

    @InjectMocks
    private WorkOrderService workOrderService;

    private WorkOrder mockWorkOrder;
    private Milestone mockMilestone;

    @BeforeEach
    public void setUp() {
        mockWorkOrder = new WorkOrder();
        mockWorkOrder.setWorkOrderId("wo-00001-test");
        mockWorkOrder.setProjectName("Anna Nagar Pothole Repair Phase 1");
        mockWorkOrder.setCategory("RoadRepair");
        mockWorkOrder.setWard("Ward 5");
        mockWorkOrder.setZone("North");
        mockWorkOrder.setBudgetAllocated(new BigDecimal("2500000.00"));
        mockWorkOrder.setBudgetConsumedTotal(BigDecimal.ZERO);
        mockWorkOrder.setStartDate(LocalDate.of(2025, 2, 1));
        mockWorkOrder.setExpectedEndDate(LocalDate.of(2025, 3, 15));
        mockWorkOrder.setStatus("Planned");
        mockWorkOrder.setDeleted(false);

        mockMilestone = new Milestone();
        mockMilestone.setMilestoneId("ms-00001-test");
        mockMilestone.setWorkOrderId("wo-00001-test");
        mockMilestone.setDescription("Site survey and pothole marking complete");
        mockMilestone.setPlannedDate(LocalDate.of(2025, 2, 7));
        mockMilestone.setBudgetConsumed(BigDecimal.ZERO);
        mockMilestone.setStatus("Pending");
        mockMilestone.setDeleted(false);
    }

    // ── createWorkOrder ───────────────────────────────────────────────────

    @Test
    public void createWorkOrder_Success() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setProjectName("Anna Nagar Pothole Repair Phase 1");
        req.setCategory("RoadRepair");
        req.setWard("Ward 5");
        req.setZone("North");
        req.setBudgetAllocated(new BigDecimal("2500000.00"));
        req.setStartDate(LocalDate.of(2025, 2, 1));
        req.setExpectedEndDate(LocalDate.of(2025, 3, 15));

        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.createWorkOrder(req);

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
    }

    @Test
    public void createWorkOrder_InvalidCategory_ThrowsException() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setCategory("InvalidCategory");
        req.setBudgetAllocated(new BigDecimal("1000000.00"));
        req.setStartDate(LocalDate.of(2025, 1, 1));
        req.setExpectedEndDate(LocalDate.of(2025, 6, 1));

        assertThrows(BadRequestException.class,
                () -> workOrderService.createWorkOrder(req));
    }

    @Test
    public void createWorkOrder_ZeroBudget_ThrowsException() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setCategory("RoadRepair");
        req.setBudgetAllocated(BigDecimal.ZERO);
        req.setStartDate(LocalDate.of(2025, 1, 1));
        req.setExpectedEndDate(LocalDate.of(2025, 6, 1));

        assertThrows(BadRequestException.class,
                () -> workOrderService.createWorkOrder(req));
    }

    @Test
    public void createWorkOrder_EndDateBeforeStartDate_ThrowsException() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest();
        req.setCategory("RoadRepair");
        req.setBudgetAllocated(new BigDecimal("1000000.00"));
        req.setStartDate(LocalDate.of(2025, 6, 1));
        req.setExpectedEndDate(LocalDate.of(2025, 1, 1));

        assertThrows(BadRequestException.class,
                () -> workOrderService.createWorkOrder(req));
    }

    // ── getAllWorkOrders ───────────────────────────────────────────────────

    @Test
    public void getAllWorkOrders_NoFilters_ReturnsAll() {
        when(workOrderRepo.findByIsDeletedFalse()).thenReturn(List.of(mockWorkOrder));

        List<WorkOrderSummaryResponse> result =
                workOrderService.getAllWorkOrders(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("wo-00001-test", result.get(0).getWorkOrderId());
    }

    @Test
    public void getAllWorkOrders_FilterByWard_ReturnsList() {
        when(workOrderRepo.findByWardAndIsDeletedFalse("Ward 5"))
                .thenReturn(List.of(mockWorkOrder));

        List<WorkOrderSummaryResponse> result =
                workOrderService.getAllWorkOrders("Ward 5", null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Ward 5", result.get(0).getWard());
    }

    @Test
    public void getAllWorkOrders_FilterByCategory_ReturnsList() {
        when(workOrderRepo.findByCategoryAndIsDeletedFalse("RoadRepair"))
                .thenReturn(List.of(mockWorkOrder));

        List<WorkOrderSummaryResponse> result =
                workOrderService.getAllWorkOrders(null, "RoadRepair", null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void getAllWorkOrders_FilterByStatus_ReturnsList() {
        when(workOrderRepo.findByStatusAndIsDeletedFalse("Planned"))
                .thenReturn(List.of(mockWorkOrder));

        List<WorkOrderSummaryResponse> result =
                workOrderService.getAllWorkOrders(null, null, "Planned");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ── getWorkOrderById ──────────────────────────────────────────────────

    @Test
    public void getWorkOrderById_Success() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));

        WorkOrderDetailResponse result = workOrderService.getWorkOrderById("wo-00001-test");

        assertNotNull(result);
        assertEquals("wo-00001-test", result.getWorkOrderId());
        assertEquals("RoadRepair", result.getCategory());
        assertEquals("Planned", result.getStatus());
    }

    @Test
    public void getWorkOrderById_NotFound_ThrowsException() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wrong-id"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.getWorkOrderById("wrong-id"));
    }

    // ── updateWorkOrder ───────────────────────────────────────────────────

    @Test
    public void updateWorkOrder_WhenPlanned_Success() {
        mockWorkOrder.setStatus("Planned");
        UpdateWorkOrderRequest req = new UpdateWorkOrderRequest();
        req.setProjectName("Updated Name");
        req.setBudgetAllocated(new BigDecimal("3000000.00"));

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.updateWorkOrder("wo-00001-test", req);

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertEquals("Updated Name", mockWorkOrder.getProjectName());
    }

    @Test
    public void updateWorkOrder_WhenInProgress_ThrowsException() {
        mockWorkOrder.setStatus("InProgress");
        UpdateWorkOrderRequest req = new UpdateWorkOrderRequest();
        req.setProjectName("Updated Name");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));

        assertThrows(BadRequestException.class,
                () -> workOrderService.updateWorkOrder("wo-00001-test", req));
    }

    // ── updateWorkOrderStatus ─────────────────────────────────────────────

    @Test
    public void updateWorkOrderStatus_Success() {
        UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
        req.setStatus("InProgress");
        req.setRemarks("Contractor mobilised");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.updateWorkOrderStatus("wo-00001-test", req);

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertEquals("InProgress", mockWorkOrder.getStatus());
    }

    @Test
    public void updateWorkOrderStatus_InvalidStatus_ThrowsException() {
        UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
        req.setStatus("InvalidStatus");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));

        assertThrows(BadRequestException.class,
                () -> workOrderService.updateWorkOrderStatus("wo-00001-test", req));
    }

    // ── assignContractor ──────────────────────────────────────────────────

    @Test
    public void assignContractor_Success() {
        AssignContractorRequest req = new AssignContractorRequest();
        req.setAssignedContractorId("user-0009-0000-0000-000000000009");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.assignContractor("wo-00001-test", req);

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertEquals("user-0009-0000-0000-000000000009",
                mockWorkOrder.getAssignedContractorId());
    }

    @Test
    public void assignContractor_BlankContractorId_ThrowsException() {
        AssignContractorRequest req = new AssignContractorRequest();
        req.setAssignedContractorId("");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));

        assertThrows(BadRequestException.class,
                () -> workOrderService.assignContractor("wo-00001-test", req));
    }

    // ── completeWorkOrder ─────────────────────────────────────────────────

    @Test
    public void completeWorkOrder_Success() {
        CompleteWorkOrderRequest req = new CompleteWorkOrderRequest();
        req.setActualEndDate(LocalDate.of(2025, 3, 10));
        req.setRemarks("All work done");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.completeWorkOrder("wo-00001-test", req);

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertEquals("Completed", mockWorkOrder.getStatus());
        assertEquals(LocalDate.of(2025, 3, 10), mockWorkOrder.getActualEndDate());
    }

    @Test
    public void completeWorkOrder_NoActualEndDate_ThrowsException() {
        CompleteWorkOrderRequest req = new CompleteWorkOrderRequest();
        // actualEndDate is null

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));

        assertThrows(BadRequestException.class,
                () -> workOrderService.completeWorkOrder("wo-00001-test", req));
    }

    // ── cancelWorkOrder ───────────────────────────────────────────────────

    @Test
    public void cancelWorkOrder_Success() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.cancelWorkOrder("wo-00001-test");

        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertTrue(mockWorkOrder.isDeleted());
        assertEquals("Cancelled", mockWorkOrder.getStatus());
    }

    // ── createMilestone ───────────────────────────────────────────────────

    @Test
    public void createMilestone_Success() {
        CreateMilestoneRequest req = new CreateMilestoneRequest();
        req.setDescription("Site survey and pothole marking complete");
        req.setPlannedDate(LocalDate.of(2025, 2, 7));

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.save(any(Milestone.class))).thenReturn(mockMilestone);

        workOrderService.createMilestone("wo-00001-test", req);

        verify(milestoneRepo, times(1)).save(any(Milestone.class));
    }

    @Test
    public void createMilestone_WorkOrderNotFound_ThrowsException() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wrong-id"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.createMilestone("wrong-id",
                        new CreateMilestoneRequest()));
    }

    // ── getMilestonesByWorkOrder ──────────────────────────────────────────

    @Test
    public void getMilestonesByWorkOrder_Success_ReturnsList() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(List.of(mockMilestone));

        List<MilestoneResponse> result =
                workOrderService.getMilestonesByWorkOrder("wo-00001-test");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ms-00001-test", result.get(0).getMilestoneId());
    }

    // ── getMilestoneById ──────────────────────────────────────────────────

    @Test
    public void getMilestoneById_Success() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));

        MilestoneResponse result =
                workOrderService.getMilestoneById("wo-00001-test", "ms-00001-test");

        assertNotNull(result);
        assertEquals("ms-00001-test", result.getMilestoneId());
    }

    @Test
    public void getMilestoneById_NotFound_ThrowsException() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("wrong-ms"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.getMilestoneById("wo-00001-test", "wrong-ms"));
    }

    // ── completeMilestone ─────────────────────────────────────────────────

    @Test
    public void completeMilestone_Success_UpdatesBudget() {
        CompleteMilestoneRequest req = new CompleteMilestoneRequest();
        req.setCompletedDate(LocalDate.of(2025, 2, 6));
        req.setBudgetConsumed(new BigDecimal("85000.00"));

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneRepo.save(any(Milestone.class))).thenReturn(mockMilestone);
        when(milestoneRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(List.of(mockMilestone));
        when(workOrderRepo.save(any(WorkOrder.class))).thenReturn(mockWorkOrder);

        workOrderService.completeMilestone("wo-00001-test", "ms-00001-test", req);

        verify(milestoneRepo, times(1)).save(any(Milestone.class));
        verify(workOrderRepo, times(1)).save(any(WorkOrder.class));
        assertEquals("Completed", mockMilestone.getStatus());
        assertEquals(LocalDate.of(2025, 2, 6), mockMilestone.getCompletedDate());
    }

    @Test
    public void completeMilestone_NoCompletedDate_ThrowsException() {
        CompleteMilestoneRequest req = new CompleteMilestoneRequest();
        // completedDate is null

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));

        assertThrows(BadRequestException.class,
                () -> workOrderService.completeMilestone(
                        "wo-00001-test", "ms-00001-test", req));
    }

    // ── updateMilestoneStatus ─────────────────────────────────────────────

    @Test
    public void updateMilestoneStatus_ToDelayed_Success() {
        UpdateMilestoneStatusRequest req = new UpdateMilestoneStatusRequest();
        req.setStatus("Delayed");
        req.setRemarks("Supply chain issue");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneRepo.save(any(Milestone.class))).thenReturn(mockMilestone);

        workOrderService.updateMilestoneStatus("wo-00001-test", "ms-00001-test", req);

        verify(milestoneRepo, times(1)).save(any(Milestone.class));
        assertEquals("Delayed", mockMilestone.getStatus());
    }

    @Test
    public void updateMilestoneStatus_InvalidStatus_ThrowsException() {
        UpdateMilestoneStatusRequest req = new UpdateMilestoneStatusRequest();
        req.setStatus("InvalidStatus");

        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));

        assertThrows(BadRequestException.class,
                () -> workOrderService.updateMilestoneStatus(
                        "wo-00001-test", "ms-00001-test", req));
    }

    // ── deleteMilestone ───────────────────────────────────────────────────

    @Test
    public void deleteMilestone_Success() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByMilestoneIdAndIsDeletedFalse("ms-00001-test"))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneRepo.save(any(Milestone.class))).thenReturn(mockMilestone);

        workOrderService.deleteMilestone("wo-00001-test", "ms-00001-test");

        verify(milestoneRepo, times(1)).save(any(Milestone.class));
        assertTrue(mockMilestone.isDeleted());
    }

    // ── getBudgetSummary ──────────────────────────────────────────────────

    @Test
    public void getBudgetSummary_Success() {
        mockWorkOrder.setBudgetConsumedTotal(new BigDecimal("85000.00"));
        when(workOrderRepo.findByIsDeletedFalse()).thenReturn(List.of(mockWorkOrder));

        List<BudgetSummaryResponse> result = workOrderService.getBudgetSummary();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("wo-00001-test", result.get(0).getWorkOrderId());
        assertTrue(result.get(0).getUtilizationPct() > 0);
    }

    // ── getPublicWorkOrdersByWard ──────────────────────────────────────────

    @Test
    public void getPublicWorkOrdersByWard_Success() {
        when(workOrderRepo.findByWardAndIsDeletedFalse("Ward 5"))
                .thenReturn(List.of(mockWorkOrder));
        when(milestoneRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(List.of(mockMilestone));

        List<PublicWorkOrderResponse> result =
                workOrderService.getPublicWorkOrdersByWard("Ward 5");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Ward 5", result.get(0).getWard());
    }

    // ── getPublicWorkOrderDetail ──────────────────────────────────────────

    @Test
    public void getPublicWorkOrderDetail_Success() {
        when(workOrderRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(Optional.of(mockWorkOrder));
        when(milestoneRepo.findByWorkOrderIdAndIsDeletedFalse("wo-00001-test"))
                .thenReturn(List.of(mockMilestone));

        PublicWorkOrderResponse result =
                workOrderService.getPublicWorkOrderDetail("wo-00001-test");

        assertNotNull(result);
        assertEquals("wo-00001-test", result.getWorkOrderId());
        assertEquals(1, result.getTotalMilestones());
        assertEquals(0, result.getCompletedMilestones());
    }
}
