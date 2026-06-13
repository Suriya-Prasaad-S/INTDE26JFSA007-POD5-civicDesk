package com.civicdesk.module.iam.entity;

import com.civicdesk.common.id.NumericStringSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;

// columnList values match the physical (camelCase) column names — this project uses
// PhysicalNamingStrategyStandardImpl, so columns are NOT snake_cased.
// Indexes back the audit-log filters (by userId / action / module) and the default
// timestamp-desc ordering, keeping queries fast as the table grows.
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_userId",    columnList = "userId"),
    @Index(name = "idx_audit_action",    columnList = "action"),
    @Index(name = "idx_audit_module",    columnList = "module"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    // Sequential numeric id rendered as a String: 10000001, 10000002, … (was a UUID).
    @Id
    @GeneratedValue(generator = "auditIdSeq")
    @GenericGenerator(
            name = "auditIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                @Parameter(name = "sequence_name", value = "audit_id_seq"),
                @Parameter(name = "initial_value", value = "10000001"),
                @Parameter(name = "increment_size", value = "1"),
                @Parameter(name = "optimizer", value = "none")
            })
    @Column(name = "auditId", length = 36, updatable = false, nullable = false)
    private String auditId;

    @Column(name = "userId", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(name = "ipAddress", length = 45)
    private String ipAddress;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
