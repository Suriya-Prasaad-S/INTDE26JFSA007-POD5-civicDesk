package com.civicdesk.module.publicworks.controller;

import com.civicdesk.module.publicworks.dto.request.*;
import com.civicdesk.module.publicworks.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/civicDesk/workorders")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    // ════════════════════════════════════════════════
    // PUBLIC WORKS ENGINEER — WorkOrder endpoints
    // ════════════════════════════════════════════════

    // #1 POST civicDesk/workorders/createWorkOrder
    @PostMapping("/createWorkOrder")
    public ResponseEntity<Map<String, Object>> createWorkOrder(
            @RequestBody CreateWorkOrderRequest req) {
        try {
            workOrderService.createWorkOrder(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Work order created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to create work order"));
        }
    }

    // #2 GET civicDesk/workorders/getAllWorkOrders?ward=&category=&status=
    @GetMapping("/getAllWorkOrders")
    public ResponseEntity<Map<String, Object>> getAllWorkOrders(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        try {
            var list = workOrderService.getAllWorkOrders(ward, category, status);
            if (list.isEmpty()) {
                return ResponseEntity.ok(msg("No work orders found"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Work orders fetched successfully");
            body.put("workOrders", list);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No work orders found"));
        }
    }

    // #3 GET civicDesk/workorders/{workOrderId}
    @GetMapping("/{workOrderId}")
    public ResponseEntity<Map<String, Object>> getWorkOrderById(
            @PathVariable String workOrderId) {
        try {
            var detail = workOrderService.getWorkOrderById(workOrderId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Work order details fetched successfully");
            body.put("workOrderId", detail.getWorkOrderId());
            body.put("projectName", detail.getProjectName());
            body.put("category", detail.getCategory());
            body.put("ward", detail.getWard());
            body.put("zone", detail.getZone());
            body.put("budgetAllocated", detail.getBudgetAllocated());
            body.put("budgetConsumedTotal", detail.getBudgetConsumedTotal());
            body.put("startDate", detail.getStartDate());
            body.put("expectedEndDate", detail.getExpectedEndDate());
            body.put("actualEndDate", detail.getActualEndDate());
            body.put("assignedContractorId", detail.getAssignedContractorId());
            body.put("assignedEngineerId", detail.getAssignedEngineerId());
            body.put("status", detail.getStatus());
            body.put("remarks", detail.getRemarks());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("Work order not found"));
        }
    }

    // #4 PUT civicDesk/workorders/{workOrderId}/update
    @PutMapping("/{workOrderId}/update")
    public ResponseEntity<Map<String, Object>> updateWorkOrder(
            @PathVariable String workOrderId,
            @RequestBody UpdateWorkOrderRequest req) {
        try {
            workOrderService.updateWorkOrder(workOrderId, req);
            return ResponseEntity.ok(msg("Work order updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update work order"));
        }
    }

    // #5 PUT civicDesk/workorders/{workOrderId}/updateStatus
    @PutMapping("/{workOrderId}/updateStatus")
    public ResponseEntity<Map<String, Object>> updateWorkOrderStatus(
            @PathVariable String workOrderId,
            @RequestBody UpdateWorkOrderStatusRequest req) {
        try {
            workOrderService.updateWorkOrderStatus(workOrderId, req);
            return ResponseEntity.ok(msg("Work order status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update work order status"));
        }
    }

    // #6 PUT civicDesk/workorders/{workOrderId}/assignContractor
    @PutMapping("/{workOrderId}/assignContractor")
    public ResponseEntity<Map<String, Object>> assignContractor(
            @PathVariable String workOrderId,
            @RequestBody AssignContractorRequest req) {
        try {
            workOrderService.assignContractor(workOrderId, req);
            return ResponseEntity.ok(msg("Contractor assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to assign contractor"));
        }
    }

    // #7 PUT civicDesk/workorders/{workOrderId}/complete
    @PutMapping("/{workOrderId}/complete")
    public ResponseEntity<Map<String, Object>> completeWorkOrder(
            @PathVariable String workOrderId,
            @RequestBody CompleteWorkOrderRequest req) {
        try {
            workOrderService.completeWorkOrder(workOrderId, req);
            return ResponseEntity.ok(msg("Work order marked as completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to complete work order"));
        }
    }

    // #8 PUT civicDesk/workorders/{workOrderId}/cancel
    @PutMapping("/{workOrderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelWorkOrder(
            @PathVariable String workOrderId) {
        try {
            workOrderService.cancelWorkOrder(workOrderId);
            return ResponseEntity.ok(msg("Work order cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to cancel work order"));
        }
    }

    // ════════════════════════════════════════════════
    // MILESTONE endpoints
    // ════════════════════════════════════════════════

    // #9 POST civicDesk/workorders/{workOrderId}/milestones/createMilestone
    @PostMapping("/{workOrderId}/milestones/createMilestone")
    public ResponseEntity<Map<String, Object>> createMilestone(
            @PathVariable String workOrderId,
            @RequestBody CreateMilestoneRequest req) {
        try {
            workOrderService.createMilestone(workOrderId, req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Milestone created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to create milestone"));
        }
    }

    // #10 GET civicDesk/workorders/{workOrderId}/milestones
    @GetMapping("/{workOrderId}/milestones")
    public ResponseEntity<Map<String, Object>> getMilestonesByWorkOrder(
            @PathVariable String workOrderId) {
        try {
            var milestones = workOrderService.getMilestonesByWorkOrder(workOrderId);
            if (milestones.isEmpty()) {
                return ResponseEntity.ok(msg("No milestones found for this work order"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Milestones fetched successfully");
            body.put("workOrderId", workOrderId);
            body.put("milestones", milestones);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No milestones found for this work order"));
        }
    }

    // #11 GET civicDesk/workorders/{workOrderId}/milestones/{milestoneId}
    @GetMapping("/{workOrderId}/milestones/{milestoneId}")
    public ResponseEntity<Map<String, Object>> getMilestoneById(
            @PathVariable String workOrderId,
            @PathVariable String milestoneId) {
        try {
            var ms = workOrderService.getMilestoneById(workOrderId, milestoneId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Milestone details fetched successfully");
            body.put("milestoneId", ms.getMilestoneId());
            body.put("workOrderId", ms.getWorkOrderId());
            body.put("description", ms.getDescription());
            body.put("plannedDate", ms.getPlannedDate());
            body.put("completedDate", ms.getCompletedDate());
            body.put("budgetConsumed", ms.getBudgetConsumed());
            body.put("status", ms.getStatus());
            body.put("remarks", ms.getRemarks());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("Milestone not found"));
        }
    }

    // #12 PUT civicDesk/workorders/{workOrderId}/milestones/{milestoneId}/update
    @PutMapping("/{workOrderId}/milestones/{milestoneId}/update")
    public ResponseEntity<Map<String, Object>> updateMilestone(
            @PathVariable String workOrderId,
            @PathVariable String milestoneId,
            @RequestBody UpdateMilestoneRequest req) {
        try {
            workOrderService.updateMilestone(workOrderId, milestoneId, req);
            return ResponseEntity.ok(msg("Milestone updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update milestone"));
        }
    }

    // #13 PUT civicDesk/workorders/{workOrderId}/milestones/{milestoneId}/complete
    @PutMapping("/{workOrderId}/milestones/{milestoneId}/complete")
    public ResponseEntity<Map<String, Object>> completeMilestone(
            @PathVariable String workOrderId,
            @PathVariable String milestoneId,
            @RequestBody CompleteMilestoneRequest req) {
        try {
            workOrderService.completeMilestone(workOrderId, milestoneId, req);
            return ResponseEntity.ok(msg("Milestone marked as completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to complete milestone"));
        }
    }

    // #14 PUT civicDesk/workorders/{workOrderId}/milestones/{milestoneId}/updateStatus
    @PutMapping("/{workOrderId}/milestones/{milestoneId}/updateStatus")
    public ResponseEntity<Map<String, Object>> updateMilestoneStatus(
            @PathVariable String workOrderId,
            @PathVariable String milestoneId,
            @RequestBody UpdateMilestoneStatusRequest req) {
        try {
            workOrderService.updateMilestoneStatus(workOrderId, milestoneId, req);
            return ResponseEntity.ok(msg("Milestone status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update milestone status"));
        }
    }

    // #15 PUT civicDesk/workorders/{workOrderId}/milestones/{milestoneId}/delete
    @PutMapping("/{workOrderId}/milestones/{milestoneId}/delete")
    public ResponseEntity<Map<String, Object>> deleteMilestone(
            @PathVariable String workOrderId,
            @PathVariable String milestoneId) {
        try {
            workOrderService.deleteMilestone(workOrderId, milestoneId);
            return ResponseEntity.ok(msg("Milestone deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to delete milestone"));
        }
    }

    // ════════════════════════════════════════════════
    // DEPARTMENT SUPERVISOR — special endpoints
    // ════════════════════════════════════════════════

    // #16 GET civicDesk/workorders/budgetSummary
    @GetMapping("/budgetSummary")
    public ResponseEntity<Map<String, Object>> getBudgetSummary() {
        try {
            var summary = workOrderService.getBudgetSummary();
            if (summary.isEmpty()) {
                return ResponseEntity.ok(msg("No data available"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Budget summary fetched successfully");
            body.put("summary", summary);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(msg("No data available"));
        }
    }

    // #17 GET civicDesk/workorders/delayedMilestones
    @GetMapping("/delayedMilestones")
    public ResponseEntity<Map<String, Object>> getDelayedMilestones() {
        try {
            var delayed = workOrderService.getDelayedMilestones();
            if (delayed.isEmpty()) {
                return ResponseEntity.ok(msg("No delayed milestones found"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Delayed milestones fetched successfully");
            body.put("delayedMilestones", delayed);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(msg("No delayed milestones found"));
        }
    }

    // ════════════════════════════════════════════════
    // CITIZEN — public read-only endpoints
    // ════════════════════════════════════════════════

    // #18 GET civicDesk/workorders/public/getByWard?ward={ward}
    @GetMapping("/public/getByWard")
    public ResponseEntity<Map<String, Object>> getPublicWorkOrdersByWard(
            @RequestParam String ward) {
        try {
            var list = workOrderService.getPublicWorkOrdersByWard(ward);
            if (list.isEmpty()) {
                return ResponseEntity.ok(msg("No active works found for this ward"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Public works orders fetched successfully");
            body.put("ward", ward);
            body.put("workOrders", list);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No active works found for this ward"));
        }
    }

    // #19 GET civicDesk/workorders/public/{workOrderId}
    @GetMapping("/public/{workOrderId}")
    public ResponseEntity<Map<String, Object>> getPublicWorkOrderDetail(
            @PathVariable String workOrderId) {
        try {
            var detail = workOrderService.getPublicWorkOrderDetail(workOrderId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Work order details fetched successfully");
            body.put("workOrderId", detail.getWorkOrderId());
            body.put("projectName", detail.getProjectName());
            body.put("category", detail.getCategory());
            body.put("ward", detail.getWard());
            body.put("startDate", detail.getStartDate());
            body.put("expectedEndDate", detail.getExpectedEndDate());
            body.put("actualEndDate", detail.getActualEndDate());
            body.put("status", detail.getStatus());
            body.put("totalMilestones", detail.getTotalMilestones());
            body.put("completedMilestones", detail.getCompletedMilestones());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("Work order not found"));
        }
    }

    private Map<String, Object> msg(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }
}
