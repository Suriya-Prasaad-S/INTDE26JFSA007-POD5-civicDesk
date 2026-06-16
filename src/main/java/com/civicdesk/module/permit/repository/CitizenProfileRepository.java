package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, String> {
    Optional<CitizenProfile> findByCitizenId(String citizenId);
}