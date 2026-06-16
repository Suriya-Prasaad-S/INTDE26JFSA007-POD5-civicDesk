package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.PermitDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermitDocumentRepository extends JpaRepository<PermitDocument, String> {

    List<PermitDocument> findByPermitIdAndIsDeletedFalse(String permitId);

    Optional<PermitDocument> findByDocumentIdAndIsDeletedFalse(String documentId);
}