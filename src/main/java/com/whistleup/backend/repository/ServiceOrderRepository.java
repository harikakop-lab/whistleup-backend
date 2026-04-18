package com.whistleup.backend.repository;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

    List<ServiceOrder> findAllByProfileId(String profileId);

    List<ServiceOrder> findAllByProfileIdAndOrderStatus(String profileId, OrderStatus orderStatus);

    List<ServiceOrder> findAllByProfileIdAndIssueStatus(String profileId, ServiceIssueStatus issueStatus);

    List<ServiceOrder> findAllByBuildingId(String buildingId);

    Optional<ServiceOrder> findByVhsBookingId(String vhsBookingId);
}