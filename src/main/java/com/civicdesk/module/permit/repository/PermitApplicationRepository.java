package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermitApplicationRepository extends JpaRepository<PermitApplication, String> {

    Optional<PermitApplication> findByPermitIdAndIsDeletedFalse(String permitId);

    List<PermitApplication> findByCitizenIdAndIsDeletedFalse(String citizenId);

    List<PermitApplication> findByIsDeletedFalse();

    List<PermitApplication> findByStatusAndIsDeletedFalse(PermitStatus status);

    List<PermitApplication> findByPermitTypeAndIsDeletedFalse(PermitType permitType);

    List<PermitApplication> findByStatusAndPermitTypeAndIsDeletedFalse(
            PermitStatus status, PermitType permitType);
}