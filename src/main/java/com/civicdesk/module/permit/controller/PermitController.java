package com.civicdesk.module.permit.controller;

import com.civicdesk.module.permit.dto.request.PermitDecisionRequest;
import com.civicdesk.module.permit.dto.request.RenewalRequest;
import com.civicdesk.module.permit.dto.request.RequestDocumentsRequest;
import com.civicdesk.module.permit.dto.request.ScheduleInspectionRequest;
import com.civicdesk.module.permit.dto.request.SubmitPermitRequest;
import com.civicdesk.module.permit.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.permit.service.PermitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/civicDesk/permits")
public class PermitController {

    @Autowired
    private PermitService permitService;

    
    // CITIZEN ENDPOINTS
 

    // #1 POST civicDesk/permits/createPermit
    @PostMapping("/createPermit")
    public ResponseEntity<Map<String, Object>> createPermit(
            @RequestBody SubmitPermitRequest req,
            @RequestHeader("X-Citizen-Id") String citizenId) {
        try {
            req.setCitizenId(citizenId);
            permitService.createPermit(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Permit application created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to create permit application"));
        }
    }

    // #2 GET civicDesk/permits/getAllPermits
    @GetMapping("/getAllPermits")
    public ResponseEntity<Map<String, Object>> getAllPermits(
            @RequestHeader("X-Citizen-Id") String citizenId) {
        try {
            var permits = permitService.getAllPermits(citizenId);
            if (permits.isEmpty()) {
                return ResponseEntity.ok(msg("No permits found"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Permits fetched successfully");
            body.put("permits", permits);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No permits found"));
        }
    }

    // #3 GET civicDesk/permits/{permitId}
    @GetMapping("/{permitId}")
    public ResponseEntity<Map<String, Object>> getPermitDetail(
            @PathVariable String permitId) {
        try {
            var detail = permitService.getPermitDetail(permitId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",         "Permit details fetched successfully");
            body.put("permitId",        detail.getPermitId());
            body.put("permitType",      detail.getPermitType());
            body.put("status",          detail.getStatus());
            body.put("applicationDate", detail.getApplicationDate());
            body.put("propertyAddress", detail.getPropertyAddress());
            body.put("ward",            detail.getWard());
            body.put("zone",            detail.getZone());
            body.put("validityPeriod",  detail.getValidityPeriod());
            body.put("validFrom",       detail.getValidFrom());
            body.put("validUntil",      detail.getValidUntil());
            body.put("fee",             detail.getFee());
            body.put("decisionDate",    detail.getDecisionDate());
            body.put("rejectionReason", detail.getRejectionReason());
            body.put("permitDetails",   detail.getPermitDetails());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("Permit not found"));
        }
    }

   
    
 // #4 POST civicDesk/permits/{permitId}/uploadDocuments
    @PostMapping("/{permitId}/uploadDocuments")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable String permitId,
            @RequestParam("documentType") List<String> documentTypes,
            @RequestParam("file") List<MultipartFile> files) {
        try {
            if (documentTypes.size() != files.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(msg("Number of documentTypes must match number of files"));
            }
            permitService.uploadDocuments(permitId, documentTypes, files);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Document uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to upload document"));
        }
    }

    // #5 GET civicDesk/permits/{permitId}/documents
    @GetMapping("/{permitId}/documents")
    public ResponseEntity<Map<String, Object>> getDocuments(
            @PathVariable String permitId) {
        try {
            var docs = permitService.getDocuments(permitId);
            if (docs.isEmpty()) {
                return ResponseEntity.ok(msg("No documents found for this permit"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",   "Documents fetched successfully");
            body.put("permitId",  permitId);
            body.put("documents", docs);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No documents found for this permit"));
        }
    }

    // #6 POST civicDesk/permits/{permitId}/renew
    @PostMapping("/{permitId}/renew")
    public ResponseEntity<Map<String, Object>> renewPermit(
            @PathVariable String permitId,
            @RequestBody RenewalRequest req) {
        try {
            permitService.renewPermit(permitId, req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Renewal application created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to create renewal application"));
        }
    }

    
    // SUPERVISOR ENDPOINTS
   
    // #7 GET civicDesk/permits/queue
    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String permitType) {
        try {
            var apps = permitService.getQueue(status, permitType);
            if (apps.isEmpty()) {
                return ResponseEntity.ok(msg("No applications found in queue"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",      "Application queue fetched successfully");
            body.put("applications", apps);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No applications found in queue"));
        }
    }

    // #8 GET civicDesk/permits/{permitId} — same as #3, shared endpoint

    // #9 PUT civicDesk/permits/{permitId}/requestDocuments
    @PutMapping("/{permitId}/requestDocuments")
    public ResponseEntity<Map<String, Object>> requestDocuments(
            @PathVariable String permitId,
            @RequestBody RequestDocumentsRequest req) {
        try {
            permitService.requestDocuments(permitId, req);
            return ResponseEntity.ok(msg("Citizen notified to re-upload documents"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to notify citizen"));
        }
    }
    
 // Download document file directly
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable String documentId) {
        try {
            byte[] fileBytes = permitService.downloadDocument(documentId);
            String fileName = permitService.getDocumentFileName(documentId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // #10 PUT civicDesk/permits/{permitId}/documents/{documentId}/verify
    @PutMapping("/{permitId}/documents/{documentId}/verify")
    public ResponseEntity<Map<String, Object>> verifyDocument(
            @PathVariable String permitId,
            @PathVariable String documentId,
            @RequestBody VerifyDocumentRequest req) {
        try {
            permitService.verifyDocument(permitId, documentId, req);
            return ResponseEntity.ok(
                    msg("Document verification status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update document verification status"));
        }
    }

    // #11 POST civicDesk/permits/{permitId}/inspections
    @PostMapping("/{permitId}/inspections")
    public ResponseEntity<Map<String, Object>> scheduleInspection(
            @PathVariable String permitId,
            @RequestBody ScheduleInspectionRequest req) {
        try {
            permitService.scheduleInspection(permitId, req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(msg("Inspection scheduled and officer notified successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to schedule inspection"));
        }
    }

    // #12 GET civicDesk/permits/{permitId}/inspections
    @GetMapping("/{permitId}/inspections")
    public ResponseEntity<Map<String, Object>> getInspections(
            @PathVariable String permitId) {
        try {
            var inspections = permitService.getInspections(permitId);
            if (inspections.isEmpty()) {
                return ResponseEntity.ok(
                        msg("No inspection records found for this permit"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",     "Inspection results fetched successfully");
            body.put("permitId",    permitId);
            body.put("inspections", inspections);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No inspection records found for this permit"));
        }
    }

    // #13 PUT civicDesk/permits/{permitId}/decision
    @PutMapping("/{permitId}/decision")
    public ResponseEntity<Map<String, Object>> makeDecision(
            @PathVariable String permitId,
            @RequestBody PermitDecisionRequest req) {
        try {
            permitService.makeDecision(permitId, req);
            return ResponseEntity.ok(msg("Permit decision updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to update permit decision"));
        }
    }

  
    // FIELD OFFICER ENDPOINTS


    // #14 GET civicDesk/permits/inspections/myAssignments
    @GetMapping("/inspections/myAssignments")
    public ResponseEntity<Map<String, Object>> getMyAssignments(
            @RequestHeader("X-Officer-Id") String officerId) {
        try {
            var inspections = permitService.getMyAssignments(officerId);
            if (inspections.isEmpty()) {
                return ResponseEntity.ok(msg("No assigned inspections found"));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",     "Assigned inspections fetched successfully");
            body.put("inspections", inspections);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("No assigned inspections found"));
        }
    }

    // #15 GET civicDesk/permits/inspections/{inspectionId}
    @GetMapping("/inspections/{inspectionId}")
    public ResponseEntity<Map<String, Object>> getInspectionDetail(
            @PathVariable String inspectionId) {
        try {
            var i = permitService.getInspectionDetail(inspectionId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message",           "Inspection details fetched successfully");
            body.put("inspectionId",      i.getInspectionId());
            body.put("permitId",          i.getPermitId());
            body.put("permitType",        i.getPermitType());
            body.put("propertyAddress",   i.getPropertyAddress());
            body.put("citizenName",       i.getCitizenName());
            body.put("assignedOfficerId", i.getAssignedOfficerId());
            body.put("scheduledDate",     i.getScheduledDate());
            body.put("conductedDate",     i.getConductedDate());
            body.put("outcome",           i.getOutcome());
            body.put("remarks",           i.getRemarks());
            body.put("gpsCoordinates",    i.getGpsCoordinates());
            body.put("photoPath",         i.getPhotoPath());
            body.put("status",            i.getStatus());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(msg("Inspection not found"));
        }
    }

    // #16 PUT civicDesk/permits/inspections/{inspectionId}/submit
    @PutMapping("/inspections/{inspectionId}/submit")
    public ResponseEntity<Map<String, Object>> submitOutcome(
            @PathVariable String inspectionId,
            @RequestParam("outcome") String outcome,
            @RequestParam(value = "remarks",        required = false) String remarks,
            @RequestParam(value = "gpsCoordinates", required = false) String gpsCoordinates,
            @RequestParam(value = "photo",          required = false) MultipartFile photo) {
        try {
            permitService.submitInspectionOutcome(
                    inspectionId, outcome, remarks, gpsCoordinates, photo);
            return ResponseEntity.ok(
                    msg("Inspection outcome submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(msg("Failed to submit inspection outcome"));
        }
    }

    private Map<String, Object> msg(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }
}