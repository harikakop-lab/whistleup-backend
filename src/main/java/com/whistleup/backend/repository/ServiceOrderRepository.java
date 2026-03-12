package com.whistleup.backend.repository;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {

    List<ServiceOrder> findAllByProfileId(String profileId);

    List<ServiceOrder> findAllByProfileIdAndOrderStatus(String profileId, OrderStatus orderStatus);
}