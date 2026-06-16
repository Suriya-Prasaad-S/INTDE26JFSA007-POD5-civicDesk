package com.civicdesk.module.permit.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.permit.dto.request.PermitDecisionRequest;
import com.civicdesk.module.permit.dto.request.RenewalRequest;
import com.civicdesk.module.permit.dto.request.RequestDocumentsRequest;
import com.civicdesk.module.permit.dto.request.ScheduleInspectionRequest;
import com.civicdesk.module.permit.dto.request.SubmitPermitRequest;
import com.civicdesk.module.permit.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.permit.dto.response.DocumentResponse;
import com.civicdesk.module.permit.dto.response.InspectionResponse;
import com.civicdesk.module.permit.dto.response.PermitDetailResponse;
import com.civicdesk.module.permit.dto.response.PermitSummaryResponse;
import com.civicdesk.module.permit.entity.Inspection;
import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.entity.PermitDocument;
import com.civicdesk.module.permit.entity.User;
import com.civicdesk.module.permit.enums.DocumentType;
import com.civicdesk.module.permit.enums.InspectionStatus;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import com.civicdesk.module.permit.repository.CitizenProfileRepository;
import com.civicdesk.module.permit.repository.InspectionRepository;
import com.civicdesk.module.permit.repository.PermitApplicationRepository;
import com.civicdesk.module.permit.repository.PermitDocumentRepository;
import com.civicdesk.module.permit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PermitService {

    @Autowired
    private PermitApplicationRepository permitRepo;

    @Autowired
    private PermitDocumentRepository documentRepo;

    @Autowired
    private InspectionRepository inspectionRepo;

    @Autowired
    private CitizenProfileRepository citizenRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private FileStorageService fileStorage;

    // ── #1 createPermit ──────────────────────────────────────────────
    public void createPermit(SubmitPermitRequest req) {
        citizenRepo.findByCitizenId(req.getCitizenId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen not found: " + req.getCitizenId()));

        PermitApplication permit = new PermitApplication();
        permit.setPermitId(UUID.randomUUID().toString());
        permit.setCitizenId(req.getCitizenId());
        permit.setPermitType(req.getPermitType());
        permit.setApplicationDate(LocalDate.now());
        permit.setPropertyAddress(req.getPropertyAddress());
        permit.setWard(req.getWard());
        permit.setZone(req.getZone());
        permit.setPermitDetails(req.getPermitDetails());
        permit.setValidityPeriod(req.getValidityPeriod());
        permit.setFee(req.getFee());
        permit.setStatus(PermitStatus.Applied);
        permit.setDeleted(false);
        permitRepo.save(permit);
    }

    // ── #2 getAllPermits ──────────────────────────────────────────────
    public List<PermitSummaryResponse> getAllPermits(String citizenId) {
        return permitRepo.findByCitizenIdAndIsDeletedFalse(citizenId)
                .stream()
                .map(p -> {
                    PermitSummaryResponse r = new PermitSummaryResponse();
                    r.setPermitId(p.getPermitId());
                    r.setPermitType(p.getPermitType().name());
                    r.setPropertyAddress(p.getPropertyAddress());
                    r.setStatus(p.getStatus().name());
                    r.setApplicationDate(p.getApplicationDate());
                    r.setValidUntil(p.getValidUntil());
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ── #3 getPermitDetail ────────────────────────────────────────────
    public PermitDetailResponse getPermitDetail(String permitId) {
        PermitApplication p = requirePermit(permitId);
        PermitDetailResponse r = new PermitDetailResponse();
        r.setPermitId(p.getPermitId());
        r.setPermitType(p.getPermitType().name());
        r.setStatus(p.getStatus().name());
        r.setApplicationDate(p.getApplicationDate());
        r.setPropertyAddress(p.getPropertyAddress());
        r.setWard(p.getWard());
        r.setZone(p.getZone());
        r.setValidityPeriod(p.getValidityPeriod());
        r.setValidFrom(p.getValidFrom());
        r.setValidUntil(p.getValidUntil());
        r.setFee(p.getFee());
        r.setDecisionDate(p.getDecisionDate());
        r.setRejectionReason(p.getRejectionReason());
        r.setPermitDetails(p.getPermitDetails());
        return r;
    }

    // ── #4 uploadDocument ─────────────────────────────────────────────
    public void uploadDocument(String permitId, String documentTypeStr, MultipartFile file) {
        PermitApplication permit = requirePermit(permitId);

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(documentTypeStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid documentType: " + documentTypeStr);
        }

        String filePath = fileStorage.store(file,
                "permits/" + permitId.replace("-", "").substring(0, 8));

        PermitDocument doc = new PermitDocument();
        doc.setDocumentId(UUID.randomUUID().toString());
        doc.setPermitId(permitId);
        doc.setDocumentType(docType);
        doc.setFilePath(filePath);
        doc.setDeleted(false);
        documentRepo.save(doc);

        if (permit.getStatus() == PermitStatus.Applied
                || permit.getStatus() == PermitStatus.PendingDocuments) {
            permit.setStatus(PermitStatus.UnderReview);
            permitRepo.save(permit);
        }
    }
    
 // #4 uploadDocuments — bulk upload
    public void uploadDocuments(String permitId, List<String> documentTypes,
                                 List<MultipartFile> files) {
        PermitApplication permit = requirePermit(permitId);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String documentTypeStr = documentTypes.get(i);

            DocumentType docType;
            try {
                docType = DocumentType.valueOf(documentTypeStr);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid documentType: " + documentTypeStr);
            }

            String filePath = fileStorage.store(file,
                    "permits/" + permitId.replace("-", "").substring(0, 8));

            PermitDocument doc = new PermitDocument();
            doc.setDocumentId(UUID.randomUUID().toString());
            doc.setPermitId(permitId);
            doc.setDocumentType(docType);
            doc.setFilePath(filePath);
            doc.setDeleted(false);
            documentRepo.save(doc);
        }

        if (permit.getStatus() == PermitStatus.Applied
                || permit.getStatus() == PermitStatus.PendingDocuments) {
            permit.setStatus(PermitStatus.UnderReview);
            permitRepo.save(permit);
        }
    }

    // ── #5 getDocuments ───────────────────────────────────────────────
    public List<DocumentResponse> getDocuments(String permitId) {
        requirePermit(permitId);
        return documentRepo.findByPermitIdAndIsDeletedFalse(permitId)
                .stream()
                .map(d -> {
                    DocumentResponse r = new DocumentResponse();
                    r.setDocumentId(d.getDocumentId());
                    r.setDocumentType(d.getDocumentType().name());
                    r.setFilePath(d.getFilePath());  
                    r.setVerificationStatus(d.getVerificationStatus());
                    r.setVerificationRemarks(d.getVerificationRemarks());
                    r.setUploadedAt(d.getUploadedAt());
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ── #6 renewPermit ────────────────────────────────────────────────
    public void renewPermit(String existingPermitId, RenewalRequest req) {
        PermitApplication existing = requirePermit(existingPermitId);

        if (existing.getPermitType() != PermitType.TradeLicense
                && existing.getPermitType() != PermitType.AdvertisementLicense) {
            throw new BadRequestException(
                    "Renewal only allowed for TradeLicense and AdvertisementLicense");
        }

        PermitApplication renewal = new PermitApplication();
        renewal.setPermitId(UUID.randomUUID().toString());
        renewal.setCitizenId(existing.getCitizenId());
        renewal.setPermitType(existing.getPermitType());
        renewal.setApplicationDate(LocalDate.now());
        renewal.setPropertyAddress(req.getPropertyAddress());
        renewal.setWard(req.getWard());
        renewal.setZone(req.getZone());
        renewal.setPermitDetails(req.getPermitDetails());
        renewal.setValidityPeriod(req.getValidityPeriod());
        renewal.setFee(req.getFee());
        renewal.setStatus(PermitStatus.Applied);
        renewal.setDeleted(false);
        permitRepo.save(renewal);
    }

    // ── #7 getQueue ───────────────────────────────────────────────────
    public List<PermitSummaryResponse> getQueue(String statusStr, String permitTypeStr) {
        List<PermitApplication> permits;

        if (statusStr != null && permitTypeStr != null) {
            permits = permitRepo.findByStatusAndPermitTypeAndIsDeletedFalse(
                    PermitStatus.valueOf(statusStr), PermitType.valueOf(permitTypeStr));
        } else if (statusStr != null) {
            permits = permitRepo.findByStatusAndIsDeletedFalse(
                    PermitStatus.valueOf(statusStr));
        } else if (permitTypeStr != null) {
            permits = permitRepo.findByPermitTypeAndIsDeletedFalse(
                    PermitType.valueOf(permitTypeStr));
        } else {
            permits = permitRepo.findByIsDeletedFalse();
        }

        return permits.stream().map(p -> {
            PermitSummaryResponse r = new PermitSummaryResponse();
            r.setPermitId(p.getPermitId());
            r.setPermitType(p.getPermitType().name());
            r.setPropertyAddress(p.getPropertyAddress());
            r.setStatus(p.getStatus().name());
            r.setApplicationDate(p.getApplicationDate());

            citizenRepo.findByCitizenId(p.getCitizenId()).ifPresent(cit ->
                userRepo.findByUserId(cit.getUserId()).ifPresent(u ->
                    r.setCitizenName(u.getName())));

            return r;
        }).collect(Collectors.toList());
    }

    // ── #9 requestDocuments ───────────────────────────────────────────
    public void requestDocuments(String permitId, RequestDocumentsRequest req) {
        PermitApplication permit = requirePermit(permitId);
        permit.setStatus(PermitStatus.PendingDocuments);
        permit.setRejectionReason(req.getRemarks());
        permitRepo.save(permit);
    }

    // ── #10 verifyDocument ────────────────────────────────────────────
    public void verifyDocument(String permitId, String documentId,
                                VerifyDocumentRequest req) {
        requirePermit(permitId);
        PermitDocument doc = documentRepo.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));
        doc.setVerificationStatus(req.getVerificationStatus());
        doc.setVerificationRemarks(req.getVerificationRemarks());
        documentRepo.save(doc);
    }

    // ── #11 scheduleInspection ────────────────────────────────────────
    public void scheduleInspection(String permitId, ScheduleInspectionRequest req) {
        PermitApplication permit = requirePermit(permitId);

        userRepo.findByUserId(req.getAssignedOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Officer not found: " + req.getAssignedOfficerId()));

        Inspection inspection = new Inspection();
        inspection.setInspectionId(UUID.randomUUID().toString());
        inspection.setPermitId(permitId);
        inspection.setAssignedOfficerId(req.getAssignedOfficerId());
        inspection.setScheduledDate(req.getScheduledDate());
        inspection.setStatus(InspectionStatus.Scheduled);
        inspectionRepo.save(inspection);

        permit.setStatus(PermitStatus.InspectionScheduled);
        permitRepo.save(permit);
    }

    // ── #12 getInspections ────────────────────────────────────────────
    public List<InspectionResponse> getInspections(String permitId) {
        requirePermit(permitId);
        return inspectionRepo.findByPermitId(permitId)
                .stream()
                .map(this::toInspectionResponse)
                .collect(Collectors.toList());
    }

    // ── #13 makeDecision ──────────────────────────────────────────────
    public void makeDecision(String permitId, PermitDecisionRequest req) {
        if (!req.getDecision().equals("Approved")
                && !req.getDecision().equals("Rejected")) {
            throw new BadRequestException(
                    "decision must be Approved or Rejected");
        }

        PermitApplication permit = requirePermit(permitId);
        PermitStatus newStatus = PermitStatus.valueOf(req.getDecision());
        permit.setStatus(newStatus);
        permit.setDecisionDate(LocalDate.now());
        permit.setRejectionReason(req.getRejectionReason());

        if (newStatus == PermitStatus.Approved
                && permit.getValidityPeriod() != null) {
            permit.setValidFrom(LocalDate.now());
            permit.setValidUntil(
                    LocalDate.now().plusMonths(permit.getValidityPeriod()));
        }
        permitRepo.save(permit);
    }

    // ── #14 getMyAssignments ──────────────────────────────────────────
    public List<InspectionResponse> getMyAssignments(String officerId) {
        return inspectionRepo.findByAssignedOfficerId(officerId)
                .stream()
                .map(i -> {
                    InspectionResponse r = new InspectionResponse();
                    r.setInspectionId(i.getInspectionId());
                    r.setPermitId(i.getPermitId());
                    r.setScheduledDate(i.getScheduledDate());
                    r.setStatus(i.getStatus().name());
                    permitRepo.findByPermitIdAndIsDeletedFalse(i.getPermitId())
                            .ifPresent(p -> {
                                r.setPermitType(p.getPermitType().name());
                                r.setPropertyAddress(p.getPropertyAddress());
                            });
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ── #15 getInspectionDetail ───────────────────────────────────────
    public InspectionResponse getInspectionDetail(String inspectionId) {
        Inspection i = inspectionRepo.findByInspectionId(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection not found: " + inspectionId));
        return toInspectionResponse(i);
    }

    // ── #16 submitInspectionOutcome ───────────────────────────────────
    public void submitInspectionOutcome(String inspectionId, String outcomeStr,
                                         String remarks, String gpsCoordinates,
                                         MultipartFile photo) {
        Inspection inspection = inspectionRepo.findByInspectionId(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection not found: " + inspectionId));

        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            photoPath = fileStorage.store(photo,
                    "inspections/" + inspectionId.replace("-", "").substring(0, 8));
        }

        inspection.setOutcome(outcomeStr);
        inspection.setRemarks(remarks);
        inspection.setGpsCoordinates(gpsCoordinates);
        inspection.setPhotoPath(photoPath);
        inspection.setConductedDate(LocalDate.now());
        inspection.setStatus(InspectionStatus.Completed);
        inspectionRepo.save(inspection);
    }

    // ── Private helpers ───────────────────────────────────────────────
    private PermitApplication requirePermit(String permitId) {
        return permitRepo.findByPermitIdAndIsDeletedFalse(permitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permit not found: " + permitId));
    }

    private InspectionResponse toInspectionResponse(Inspection i) {
        InspectionResponse r = new InspectionResponse();
        r.setInspectionId(i.getInspectionId());
        r.setPermitId(i.getPermitId());
        r.setAssignedOfficerId(i.getAssignedOfficerId());
        r.setScheduledDate(i.getScheduledDate());
        r.setConductedDate(i.getConductedDate());
        r.setOutcome(i.getOutcome());
        r.setRemarks(i.getRemarks());
        r.setGpsCoordinates(i.getGpsCoordinates());
        r.setPhotoPath(i.getPhotoPath());
        r.setStatus(i.getStatus().name());

        permitRepo.findByPermitIdAndIsDeletedFalse(i.getPermitId()).ifPresent(p -> {
            r.setPermitType(p.getPermitType().name());
            r.setPropertyAddress(p.getPropertyAddress());
            citizenRepo.findByCitizenId(p.getCitizenId()).ifPresent(cit ->
                userRepo.findByUserId(cit.getUserId()).ifPresent(u ->
                    r.setCitizenName(u.getName())));
        });
        return r;
    }
    
 // Download document file by documentId
    public byte[] downloadDocument(String documentId) {
        PermitDocument doc = documentRepo.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));

        String filePath = doc.getFilePath();
        // filePath looks like /uploads/permits/f5179bfc/site_plan.pdf
        // remove the leading slash
        String relativePath = filePath.startsWith("/")
                ? filePath.substring(1)
                : filePath;

        try {
            java.nio.file.Path path = java.nio.file.Paths.get(relativePath);
            return java.nio.file.Files.readAllBytes(path);
        } catch (java.io.IOException e) {
            throw new ResourceNotFoundException(
                    "File not found on server: " + filePath);
        }
    }

    // Get just the filename from the path
    public String getDocumentFileName(String documentId) {
        PermitDocument doc = documentRepo.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));

        String filePath = doc.getFilePath();
        // Extract filename from path like /uploads/permits/f5179bfc/site_plan.pdf
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
}