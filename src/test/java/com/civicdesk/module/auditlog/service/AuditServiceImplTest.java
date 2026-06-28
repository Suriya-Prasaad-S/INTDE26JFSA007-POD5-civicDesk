package com.civicdesk.module.auditlog.service;

import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.auditlog.dto.response.AuditLogResponse;
import com.civicdesk.module.auditlog.entity.AuditLog;
import com.civicdesk.module.auditlog.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private AuditLog capturedSave() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- log(...) -----------------------------------------------------------

    @Test
    void log_savesEntryWithGivenFields() {
        auditService.log("user-1", "LOGIN", "IAM", "10.0.0.1");

        AuditLog saved = capturedSave();
        assertEquals("user-1", saved.getUserId());
        assertEquals("LOGIN", saved.getAction());
        assertEquals("IAM", saved.getModule());
        assertEquals("10.0.0.1", saved.getIpAddress());
    }

    @Test
    void log_savesExactlyOnce() {
        auditService.log("user-1", "LOGIN", "IAM", "10.0.0.1");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void log_upperCasesAction() {
        auditService.log("user-1", "login", "IAM", "10.0.0.1");
        assertEquals("LOGIN", capturedSave().getAction());
    }

    @Test
    void log_upperCasesModule() {
        auditService.log("user-1", "LOGIN", "iam", "10.0.0.1");
        assertEquals("IAM", capturedSave().getModule());
    }

    @Test
    void log_trimsAction() {
        auditService.log("user-1", "  LOGIN  ", "IAM", "10.0.0.1");
        assertEquals("LOGIN", capturedSave().getAction());
    }

    @Test
    void log_trimsModule() {
        auditService.log("user-1", "LOGIN", "  IAM  ", "10.0.0.1");
        assertEquals("IAM", capturedSave().getModule());
    }

    @Test
    void log_passesIpVerbatim() {
        auditService.log("user-1", "LOGIN", "IAM", "203.0.113.7");
        assertEquals("203.0.113.7", capturedSave().getIpAddress());
    }

    @Test
    void log_nullActionStoredAsNull() {
        auditService.log("user-1", null, "IAM", "10.0.0.1");
        assertNull(capturedSave().getAction());
    }

    @Test
    void log_nullModuleStoredAsNull() {
        auditService.log("user-1", "LOGIN", null, "10.0.0.1");
        assertNull(capturedSave().getModule());
    }

    @Test
    void log_doesNotSetAuditIdManually() {
        auditService.log("user-1", "LOGIN", "IAM", "10.0.0.1");
        assertNull(capturedSave().getAuditId());
    }

    // ---- create(...) --------------------------------------------------------

    @Test
    void create_savesEntryWithGivenFields() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.create("user-2", "REGISTER", "GRIEVANCE", "10.0.0.2");

        AuditLog saved = capturedSave();
        assertEquals("user-2", saved.getUserId());
        assertEquals("REGISTER", saved.getAction());
        assertEquals("GRIEVANCE", saved.getModule());
        assertEquals("10.0.0.2", saved.getIpAddress());
    }

    @Test
    void create_returnsResponseMappedFromSavedEntity() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog a = inv.getArgument(0);
            a.setAuditId("10000099");
            a.setTimestamp(LocalDateTime.now());
            return a;
        });

        AuditLogResponse response = auditService.create("user-2", "REGISTER", "GRIEVANCE", "10.0.0.2");

        assertEquals("10000099", response.getAuditId());
        assertEquals("user-2", response.getUserId());
        assertEquals("REGISTER", response.getAction());
        assertEquals("GRIEVANCE", response.getModule());
        assertEquals("10.0.0.2", response.getIpAddress());
    }

    @Test
    void create_upperCasesAction() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        AuditLogResponse response = auditService.create("user-2", "register", "GRIEVANCE", "10.0.0.2");
        assertEquals("REGISTER", response.getAction());
    }

    @Test
    void create_upperCasesModule() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        AuditLogResponse response = auditService.create("user-2", "REGISTER", "grievance", "10.0.0.2");
        assertEquals("GRIEVANCE", response.getModule());
    }

    @Test
    void create_savesExactlyOnce() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        auditService.create("user-2", "REGISTER", "GRIEVANCE", "10.0.0.2");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ---- getById(...) -------------------------------------------------------

    @Test
    void getById_found_returnsMappedResponse() {
        AuditLog entry = entry("10000050", "user-3", "LOGOUT", "IAM");
        when(auditLogRepository.findById("10000050")).thenReturn(Optional.of(entry));

        AuditLogResponse response = auditService.getById("10000050");

        assertEquals("10000050", response.getAuditId());
        assertEquals("user-3", response.getUserId());
        assertEquals("LOGOUT", response.getAction());
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(auditLogRepository.findById("missing")).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> auditService.getById("missing"));
        assertEquals("Audit log not found", ex.getMessage());
    }

    @Test
    void getById_notFound_doesNotSave() {
        when(auditLogRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> auditService.getById("missing"));
        verify(auditLogRepository, never()).save(any());
    }

    // ---- getAll(...) --------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getAll_sortsByTimestampDescending() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        auditService.getAll(null, null, null, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "timestamp"), captor.getValue().getSort());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_usesGivenPageAndSize() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        auditService.getAll(null, null, null, 3, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(3, captor.getValue().getPageNumber());
        assertEquals(25, captor.getValue().getPageSize());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_mapsContentToResponses() {
        Page<AuditLog> page = new PageImpl<>(
                List.of(entry("1", "user-1", "LOGIN", "IAM")),
                PageRequest.of(0, 20), 1);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditLogResponse> result = auditService.getAll(null, null, null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("user-1", result.getContent().get(0).getUserId());
        assertEquals("LOGIN", result.getContent().get(0).getAction());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_blankFiltersAreAccepted() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<AuditLogResponse> result = auditService.getAll("  ", "", "   ", 0, 20);

        assertTrue(result.getContent().isEmpty());
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_withAllFiltersStillQueriesOnce() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        auditService.getAll("user-1", "LOGIN", "IAM", 0, 20);

        verify(auditLogRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    private AuditLog entry(String auditId, String userId, String action, String module) {
        AuditLog log = new AuditLog();
        log.setAuditId(auditId);
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress("127.0.0.1");
        log.setTimestamp(LocalDateTime.now());
        return log;
    }
}
