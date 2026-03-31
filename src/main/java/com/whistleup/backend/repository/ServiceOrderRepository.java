package com.whistleup.backend.repository;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    List<ServiceOrder> findAllByProfileId(String profileId);

    List<ServiceOrder> findAllByProfileIdAndOrderStatus(String profileId, OrderStatus orderStatus);

    List<ServiceOrder> findAllByProfileIdAndIssueStatus(String profileId, ServiceIssueStatus issueStatus);

    List<ServiceOrder> findAllByBuildingId(String buildingId);

    List<ServiceOrder> findAllByOrderStatusAndServicePersonIsNullAndServiceCityIgnoreCaseAndOrderTypeIn(
            OrderStatus orderStatus,
            String serviceCity,
            Set<ServiceOrderType> orderTypes
    );

    Optional<ServiceOrder> findByVhsBookingId(String vhsBookingId);
}