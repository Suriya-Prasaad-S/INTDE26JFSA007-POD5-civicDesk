package com.civicdesk.module.citizen.repository;

import com.civicdesk.module.citizen.entity.CitizenDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link CitizenDocument}.
 */
@Repository
public interface CitizenDocumentRepository extends JpaRepository<CitizenDocument, String> {

    /** Backs GET /{citizenId}/getAllDocuments. */
    List<CitizenDocument> findByCitizenId(String citizenId);

    /** Backs the "max 5 documents per citizen" rule. */
    long countByCitizenId(String citizenId);

    /** Backs GET /{citizenId}/getDocumentById/{documentId}, scoped to the owning citizen. */
    Optional<CitizenDocument> findByDocumentIdAndCitizenId(String documentId, String citizenId);
}
