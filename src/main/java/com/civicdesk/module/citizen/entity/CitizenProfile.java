package com.civicdesk.module.citizen.entity;

import com.civicdesk.module.citizen.entity.converter.CitizenStatusConverter;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.entity.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Citizen-specific data for a {@code User} whose role is {@code CIT}. Maps to the
 * {@code citizen_profile} table.
 *
 * <p><b>Shared primary key.</b> {@code userId} is both the PK and the link to IAM's
 * {@code User} — it holds the very same value as {@code User.userId} (1:1). It is set from the
 * authenticated caller (the JWT), never generated here. Identity/auth fields (name, email,
 * phone) live only on {@code User}; this entity holds only the citizen-specific extras.
 *
 * <p>The extra fields (date of birth, gender, national id, address, ward, zone) are
 * <em>nullable</em>: the row is created as a stub on the citizen's first visit and the extras
 * are filled in later via the "complete profile" form, after an officer has verified them.
 * {@code status} persists as a single-character code (A/V/F) via {@link CitizenStatusConverter}.
 */
@Entity
@Table(
        name = "citizen_profile",
        // Backs CitizenProfileRepository.findByWard (officer ward listing).
        indexes = @Index(name = "idx_citizen_profile_ward", columnList = "ward")
)
public class CitizenProfile {

    /** Same value as {@code User.userId}; set from the JWT at stub creation. */
    @Id
    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    @Check(constraints = "gender in ('Male','Female','Other')")
    private Gender gender;

    @Column(name = "national_id_number", unique = true, length = 50)
    private String nationalIdNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "ward")
    private String ward;

    @Column(name = "zone")
    private String zone;

    @Convert(converter = CitizenStatusConverter.class)
    @Column(name = "status", nullable = false, length = 1)
    @Check(constraints = "status in ('A','V','F')")
    private CitizenStatus status;

    /** The userId that created this profile row (the citizen themselves). */
    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    /** The officer's userId who verified (or flagged) this citizen; null until verified. */
    @Column(name = "verified_by", length = 36)
    private String verifiedBy;

    /** When the citizen was verified (or flagged); null until then. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public CitizenStatus getStatus() {
        return status;
    }

    public void setStatus(CitizenStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
