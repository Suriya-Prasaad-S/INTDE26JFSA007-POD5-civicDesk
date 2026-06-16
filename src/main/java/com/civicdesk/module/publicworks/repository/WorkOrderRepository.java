package com.civicdesk.module.publicworks.repository;

import com.civicdesk.module.publicworks.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {

    Optional<WorkOrder> findByWorkOrderIdAndIsDeletedFalse(String workOrderId);

    List<WorkOrder> findByIsDeletedFalse();

    List<WorkOrder> findByWardAndIsDeletedFalse(String ward);

    List<WorkOrder> findByCategoryAndIsDeletedFalse(String category);

    List<WorkOrder> findByStatusAndIsDeletedFalse(String status);

    List<WorkOrder> findByWardAndCategoryAndIsDeletedFalse(String ward, String category);

    List<WorkOrder> findByWardAndStatusAndIsDeletedFalse(String ward, String status);

    List<WorkOrder> findByCategoryAndStatusAndIsDeletedFalse(String category, String status);

    List<WorkOrder> findByWardAndCategoryAndStatusAndIsDeletedFalse(
            String ward, String category, String status);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.isDeleted = false")
    List<WorkOrder> findAllActive();
}
