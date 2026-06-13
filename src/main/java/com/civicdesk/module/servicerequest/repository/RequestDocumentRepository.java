package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.RequestDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestDocumentRepository extends JpaRepository<RequestDocument, String> {

    /** All documents uploaded against a request (getRequest details / getDocuments). */
    List<RequestDocument> findByRequest_RequestId(String requestId);
}
