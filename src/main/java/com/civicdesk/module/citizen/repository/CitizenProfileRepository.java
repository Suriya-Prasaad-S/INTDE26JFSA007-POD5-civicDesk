package com.civicdesk.module.citizen.repository;

import com.civicdesk.module.citizen.entity.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for {@link CitizenProfile}. Spring Data derives the SQL from the method names,
 * and {@link JpaRepository} provides the standard CRUD operations (save, findById, findAll, ...).
 */
@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, String> {

    /** Backs the "email already in use" (409) check at registration. */
    boolean existsByEmail(String email);

    /** Backs the "national ID already registered" (409) check at registration. */
    boolean existsByNationalIdNumber(String nationalIdNumber);

    /** Backs GET /getCitizensByWard/{ward}. */
    List<CitizenProfile> findByWard(String ward);
}
