package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Read/seed access to the placeholder {@code citizen_profile} table. See
 * {@link CitizenProfile} for the temporary-placeholder note.
 */
@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, String> {
}
