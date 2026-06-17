package com.civicdesk.module.citizen.repository;

import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link CitizenProfile}. The id is the citizen's {@code userId} (shared PK with
 * IAM's {@code User}). Spring Data derives the SQL from the method names.
 */
@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, String> {

    /** Backs the "national ID already registered" (409) check — matched on the SHA-256 hash. */
    boolean existsByNationalIdHash(String nationalIdHash);

    /** Backs the officer ward listing. */
    List<CitizenProfile> findByWard(String ward);

    /** Backs the pending-verification queue (citizens with status {@code Active}). */
    List<CitizenProfile> findByStatus(CitizenStatus status);
}
