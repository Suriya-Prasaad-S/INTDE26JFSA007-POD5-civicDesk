package com.civicdesk.module.publicworks.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.publicworks.dto.request.*;
import com.civicdesk.module.publicworks.dto.response.*;
import com.civicdesk.module.publicworks.entity.Milestone;
import com.civicdesk.module.publicworks.entity.WorkOrder;
import com.civicdesk.module.publicworks.repository.MilestoneRepository;
import com.civicdesk.module.publicworks.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private static final List<String> VALID_CATEGORIES = Arrays.asList(
            "RoadRepair", "Drainage", "Lighting", "SanitationUnit", "ParkMaintenance");

    private static final List<String> VALID_STATUSES = Arrays.asList(
            "Planned", "InProgress", "Completed", "OnHold", "Cancelled");

    private static final List<String> VALID_MILESTONE_STATUSES = Arrays.asList(
            "Pending", "Completed", "Delayed");

    @Autowired
    private WorkOrderRepository workOrderRepo;

    @Autowired
    private MilestoneRepository milestoneRepo;

    // ── #1 createWorkOrder ─────────────────────────────────────────────────
    public void createWorkOrder(CreateWorkOrderRequest req) {
        if (!VALID_CATEGORIES.contains(req.getCategory())) {
            throw new BadRequestException("Invalid category: " + req.getCategory()
                    + ". Must be one of: " + VALID_CATEGORIES);
        }
        if (req.getBudgetAllocated() == null || req.getBudgetAllocated().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("budgetAllocated must be greater than 0");
        }
        if (req.getExpectedEndDate() != null && req.getStartDate() != null
                && req.getExpectedEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("expectedEndDate must be on or after startDate");
        }

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderId(UUID.randomUUID().toString());
        wo.setProjectName(req.getProjectName());
        wo.setCategory(req.getCategory());
        wo.setWard(req.getWard());
        wo.setZone(req.getZone());
        wo.setBudgetAllocated(req.getBudgetAllocated());
        wo.setBudgetConsumedTotal(BigDecimal.ZERO);
        wo.setStartDate(req.getStartDate());
        wo.setExpectedEndDate(req.getExpectedEndDate());
        wo.setRemarks(req.getRemarks());
        wo.setStatus("Planned");
        wo.setDeleted(false);
        workOrderRepo.save(wo);
    }

    // ── #2 getAllWorkOrders ─────────────────────────────────────────────────
    public List<WorkOrderSummaryResponse> getAllWorkOrders(String ward, String category, String status) {
        List<WorkOrder> list;
        if (ward != null && category != null && status != null) {
            list = workOrderRepo.findByWardAndCategoryAndStatusAndIsDeletedFalse(ward, category, status);
        } else if (ward != null && category != null) {
            list = workOrderRepo.findByWardAndCategoryAndIsDeletedFalse(ward, category);
        } else if (ward != null && status != null) {
            list = workOrderRepo.findByWardAndStatusAndIsDeletedFalse(ward, status);
        } else if (category != null && status != null) {
            list = workOrderRepo.findByCategoryAndStatusAndIsDeletedFalse(category, status);
        } else if (ward != null) {
            list = workOrderRepo.findByWardAndIsDeletedFalse(ward);
        } else if (category != null) {
            list = workOrderRepo.findByCategoryAndIsDeletedFalse(category);
        } else if (status != null) {
            list = workOrderRepo.findByStatusAndIsDeletedFalse(status);
        } else {
            list = workOrderRepo.findByIsDeletedFalse();
        }
        return list.stream().map(this::toSummary).collect(Collectors.toList());
    }

    // ── #3 getWorkOrderById ─────────────────────────────────────────────────
    public WorkOrderDetailResponse getWorkOrderById(String workOrderId) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        return toDetail(wo);
    }

    // ── #4 updateWorkOrder ─────────────────────────────────────────────────
    public void updateWorkOrder(String workOrderId, UpdateWorkOrderRequest req) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        if (!"Planned".equals(wo.getStatus()) && !"OnHold".equals(wo.getStatus())) {
            throw new BadRequestException(
                    "Work order can only be updated when status is Planned or OnHold");
        }
        if (req.getProjectName() != null) wo.setProjectName(req.getProjectName());
        if (req.getBudgetAllocated() != null) {
            if (req.getBudgetAllocated().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("budgetAllocated must be greater than 0");
            }
            wo.setBudgetAllocated(req.getBudgetAllocated());
        }
        if (req.getExpectedEndDate() != null) wo.setExpectedEndDate(req.getExpectedEndDate());
        if (req.getRemarks() != null) wo.setRemarks(req.getRemarks());
        workOrderRepo.save(wo);
    }

    // ── #5 updateWorkOrderStatus ──────────────────────────────────────────
    public void updateWorkOrderStatus(String workOrderId, UpdateWorkOrderStatusRequest req) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        if (!VALID_STATUSES.contains(req.getStatus())) {
            throw new BadRequestException("Invalid status: " + req.getStatus());
        }
        wo.setStatus(req.getStatus());
        if (req.getRemarks() != null) wo.setRemarks(req.getRemarks());
        workOrderRepo.save(wo);
    }

    // ── #6 assignContractor ───────────────────────────────────────────────
    public void assignContractor(String workOrderId, AssignContractorRequest req) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        if (req.getAssignedContractorId() == null || req.getAssignedContractorId().isBlank()) {
            throw new BadRequestException("assignedContractorId must not be blank");
        }
        wo.setAssignedContractorId(req.getAssignedContractorId());
        workOrderRepo.save(wo);
    }

    // ── #7 completeWorkOrder ──────────────────────────────────────────────
    public void completeWorkOrder(String workOrderId, CompleteWorkOrderRequest req) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        if (req.getActualEndDate() == null) {
            throw new BadRequestException("actualEndDate is required");
        }
        wo.setActualEndDate(req.getActualEndDate());
        wo.setStatus("Completed");
        if (req.getRemarks() != null) wo.setRemarks(req.getRemarks());
        workOrderRepo.save(wo);
    }

    // ── #8 cancelWorkOrder ────────────────────────────────────────────────
    public void cancelWorkOrder(String workOrderId) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        wo.setDeleted(true);
        wo.setStatus("Cancelled");
        workOrderRepo.save(wo);
    }

    // ── #9 createMilestone ────────────────────────────────────────────────
    public void createMilestone(String workOrderId, CreateMilestoneRequest req) {
        requireWorkOrder(workOrderId);
        Milestone ms = new Milestone();
        ms.setMilestoneId(UUID.randomUUID().toString());
        ms.setWorkOrderId(workOrderId);
        ms.setDescription(req.getDescription());
        ms.setPlannedDate(req.getPlannedDate());
        ms.setRemarks(req.getRemarks());
        ms.setBudgetConsumed(BigDecimal.ZERO);
        ms.setStatus("Pending");
        ms.setDeleted(false);
        milestoneRepo.save(ms);
    }

    // ── #10 getMilestonesByWorkOrder ──────────────────────────────────────
    public List<MilestoneResponse> getMilestonesByWorkOrder(String workOrderId) {
        requireWorkOrder(workOrderId);
        return milestoneRepo.findByWorkOrderIdAndIsDeletedFalse(workOrderId)
                .stream().map(this::toMilestoneResponse).collect(Collectors.toList());
    }

    // ── #11 getMilestoneById ──────────────────────────────────────────────
    public MilestoneResponse getMilestoneById(String workOrderId, String milestoneId) {
        requireWorkOrder(workOrderId);
        Milestone ms = requireMilestone(milestoneId);
        if (!ms.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Milestone does not belong to the given work order");
        }
        return toMilestoneResponse(ms);
    }

    // ── #12 updateMilestone ───────────────────────────────────────────────
    public void updateMilestone(String workOrderId, String milestoneId,
                                 UpdateMilestoneRequest req) {
        requireWorkOrder(workOrderId);
        Milestone ms = requireMilestone(milestoneId);
        if (!ms.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Milestone does not belong to the given work order");
        }
        if (req.getDescription() != null) ms.setDescription(req.getDescription());
        if (req.getPlannedDate() != null) ms.setPlannedDate(req.getPlannedDate());
        if (req.getRemarks() != null) ms.setRemarks(req.getRemarks());
        milestoneRepo.save(ms);
    }

    // ── #13 completeMilestone ─────────────────────────────────────────────
    public void completeMilestone(String workOrderId, String milestoneId,
                                   CompleteMilestoneRequest req) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        Milestone ms = requireMilestone(milestoneId);
        if (!ms.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Milestone does not belong to the given work order");
        }
        if (req.getCompletedDate() == null) {
            throw new BadRequestException("completedDate is required");
        }
        BigDecimal consumed = req.getBudgetConsumed() != null ? req.getBudgetConsumed() : BigDecimal.ZERO;
        ms.setCompletedDate(req.getCompletedDate());
        ms.setBudgetConsumed(consumed);
        ms.setStatus("Completed");
        if (req.getRemarks() != null) ms.setRemarks(req.getRemarks());
        milestoneRepo.save(ms);

        // Recalculate total budget consumed for the work order
        BigDecimal total = milestoneRepo.findByWorkOrderIdAndIsDeletedFalse(workOrderId)
                .stream()
                .map(Milestone::getBudgetConsumed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wo.setBudgetConsumedTotal(total);
        workOrderRepo.save(wo);
    }

    // ── #14 updateMilestoneStatus ─────────────────────────────────────────
    public void updateMilestoneStatus(String workOrderId, String milestoneId,
                                       UpdateMilestoneStatusRequest req) {
        requireWorkOrder(workOrderId);
        Milestone ms = requireMilestone(milestoneId);
        if (!ms.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Milestone does not belong to the given work order");
        }
        if (!VALID_MILESTONE_STATUSES.contains(req.getStatus())) {
            throw new BadRequestException("Invalid milestone status: " + req.getStatus());
        }
        ms.setStatus(req.getStatus());
        if (req.getRemarks() != null) ms.setRemarks(req.getRemarks());
        milestoneRepo.save(ms);
    }

    // ── #15 deleteMilestone ───────────────────────────────────────────────
    public void deleteMilestone(String workOrderId, String milestoneId) {
        requireWorkOrder(workOrderId);
        Milestone ms = requireMilestone(milestoneId);
        if (!ms.getWorkOrderId().equals(workOrderId)) {
            throw new BadRequestException("Milestone does not belong to the given work order");
        }
        ms.setDeleted(true);
        milestoneRepo.save(ms);
    }

    // ── #16 getBudgetSummary ──────────────────────────────────────────────
    public List<BudgetSummaryResponse> getBudgetSummary() {
        return workOrderRepo.findByIsDeletedFalse().stream()
                .map(wo -> {
                    BudgetSummaryResponse r = new BudgetSummaryResponse();
                    r.setWorkOrderId(wo.getWorkOrderId());
                    r.setProjectName(wo.getProjectName());
                    r.setWard(wo.getWard());
                    r.setBudgetAllocated(wo.getBudgetAllocated());
                    r.setBudgetConsumedTotal(wo.getBudgetConsumedTotal());
                    double pct = 0.0;
                    if (wo.getBudgetAllocated().compareTo(BigDecimal.ZERO) > 0) {
                        pct = wo.getBudgetConsumedTotal()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(wo.getBudgetAllocated(), 2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    r.setUtilizationPct(pct);
                    r.setStatus(wo.getStatus());
                    return r;
                }).collect(Collectors.toList());
    }

    // ── #17 getDelayedMilestones ──────────────────────────────────────────
    public List<DelayedMilestoneResponse> getDelayedMilestones() {
        LocalDate today = LocalDate.now();
        return milestoneRepo.findByIsDeletedFalse().stream()
                .filter(ms -> "Delayed".equals(ms.getStatus())
                        || ("Pending".equals(ms.getStatus()) && ms.getPlannedDate().isBefore(today)))
                .map(ms -> {
                    DelayedMilestoneResponse r = new DelayedMilestoneResponse();
                    r.setMilestoneId(ms.getMilestoneId());
                    r.setWorkOrderId(ms.getWorkOrderId());
                    r.setDescription(ms.getDescription());
                    r.setPlannedDate(ms.getPlannedDate());
                    r.setDaysOverdue(ChronoUnit.DAYS.between(ms.getPlannedDate(), today));
                    r.setStatus(ms.getStatus());
                    r.setRemarks(ms.getRemarks());
                    workOrderRepo.findByWorkOrderIdAndIsDeletedFalse(ms.getWorkOrderId())
                            .ifPresent(wo -> r.setProjectName(wo.getProjectName()));
                    return r;
                }).collect(Collectors.toList());
    }

    // ── #18 getPublicWorkOrdersByWard ─────────────────────────────────────
    public List<PublicWorkOrderResponse> getPublicWorkOrdersByWard(String ward) {
        return workOrderRepo.findByWardAndIsDeletedFalse(ward)
                .stream().map(wo -> {
                    PublicWorkOrderResponse r = new PublicWorkOrderResponse();
                    r.setWorkOrderId(wo.getWorkOrderId());
                    r.setProjectName(wo.getProjectName());
                    r.setCategory(wo.getCategory());
                    r.setWard(wo.getWard());
                    r.setStartDate(wo.getStartDate());
                    r.setExpectedEndDate(wo.getExpectedEndDate());
                    r.setActualEndDate(wo.getActualEndDate());
                    r.setStatus(wo.getStatus());
                    List<Milestone> milestones =
                            milestoneRepo.findByWorkOrderIdAndIsDeletedFalse(wo.getWorkOrderId());
                    r.setTotalMilestones(milestones.size());
                    r.setCompletedMilestones((int) milestones.stream()
                            .filter(m -> "Completed".equals(m.getStatus())).count());
                    return r;
                }).collect(Collectors.toList());
    }

    // ── #19 getPublicWorkOrderDetail ──────────────────────────────────────
    public PublicWorkOrderResponse getPublicWorkOrderDetail(String workOrderId) {
        WorkOrder wo = requireWorkOrder(workOrderId);
        PublicWorkOrderResponse r = new PublicWorkOrderResponse();
        r.setWorkOrderId(wo.getWorkOrderId());
        r.setProjectName(wo.getProjectName());
        r.setCategory(wo.getCategory());
        r.setWard(wo.getWard());
        r.setStartDate(wo.getStartDate());
        r.setExpectedEndDate(wo.getExpectedEndDate());
        r.setActualEndDate(wo.getActualEndDate());
        r.setStatus(wo.getStatus());
        List<Milestone> milestones = milestoneRepo.findByWorkOrderIdAndIsDeletedFalse(workOrderId);
        r.setTotalMilestones(milestones.size());
        r.setCompletedMilestones((int) milestones.stream()
                .filter(m -> "Completed".equals(m.getStatus())).count());
        return r;
    }

    // ── Private helpers ───────────────────────────────────────────────────
    private WorkOrder requireWorkOrder(String workOrderId) {
        return workOrderRepo.findByWorkOrderIdAndIsDeletedFalse(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Work order not found: " + workOrderId));
    }

    private Milestone requireMilestone(String milestoneId) {
        return milestoneRepo.findByMilestoneIdAndIsDeletedFalse(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + milestoneId));
    }

    private WorkOrderSummaryResponse toSummary(WorkOrder wo) {
        WorkOrderSummaryResponse r = new WorkOrderSummaryResponse();
        r.setWorkOrderId(wo.getWorkOrderId());
        r.setProjectName(wo.getProjectName());
        r.setCategory(wo.getCategory());
        r.setWard(wo.getWard());
        r.setStatus(wo.getStatus());
        r.setStartDate(wo.getStartDate());
        r.setExpectedEndDate(wo.getExpectedEndDate());
        r.setBudgetAllocated(wo.getBudgetAllocated());
        r.setBudgetConsumedTotal(wo.getBudgetConsumedTotal());
        return r;
    }

    private WorkOrderDetailResponse toDetail(WorkOrder wo) {
        WorkOrderDetailResponse r = new WorkOrderDetailResponse();
        r.setWorkOrderId(wo.getWorkOrderId());
        r.setProjectName(wo.getProjectName());
        r.setCategory(wo.getCategory());
        r.setWard(wo.getWard());
        r.setZone(wo.getZone());
        r.setBudgetAllocated(wo.getBudgetAllocated());
        r.setBudgetConsumedTotal(wo.getBudgetConsumedTotal());
        r.setStartDate(wo.getStartDate());
        r.setExpectedEndDate(wo.getExpectedEndDate());
        r.setActualEndDate(wo.getActualEndDate());
        r.setAssignedContractorId(wo.getAssignedContractorId());
        r.setAssignedEngineerId(wo.getAssignedEngineerId());
        r.setStatus(wo.getStatus());
        r.setRemarks(wo.getRemarks());
        return r;
    }

    private MilestoneResponse toMilestoneResponse(Milestone ms) {
        MilestoneResponse r = new MilestoneResponse();
        r.setMilestoneId(ms.getMilestoneId());
        r.setWorkOrderId(ms.getWorkOrderId());
        r.setDescription(ms.getDescription());
        r.setPlannedDate(ms.getPlannedDate());
        r.setCompletedDate(ms.getCompletedDate());
        r.setBudgetConsumed(ms.getBudgetConsumed());
        r.setStatus(ms.getStatus());
        r.setRemarks(ms.getRemarks());
        return r;
    }

    // Expose for tests
    public List<Milestone> findMilestonesByWorkOrderIdAndIsDeletedFalse(String workOrderId) {
        return milestoneRepo.findByWorkOrderIdAndIsDeletedFalse(workOrderId);
    }
}
