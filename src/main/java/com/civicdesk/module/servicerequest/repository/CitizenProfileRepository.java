package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Read/seed access to the placeholder {@code citizen_profile} table. See
 * {@link CitizenProfile} for the temporary-placeholder note.
 */
@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, String> {

    /**
     * Resolve the citizen profile linked to an IAM user id. Used by the access guard to map
     * the authenticated principal (JWT {@code userId}) to its {@code citizenId} for the
     * "own resource only" ownership checks.
     */
    Optional<CitizenProfile> findByUserId(String userId);
}
