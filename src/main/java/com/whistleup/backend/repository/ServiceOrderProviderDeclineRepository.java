package com.whistleup.backend.repository;

import com.whistleup.backend.entity.ServiceOrderProviderDecline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServiceOrderProviderDeclineRepository extends JpaRepository<ServiceOrderProviderDecline, Long> {

    boolean existsByOrderIdAndServicePersonId(Long orderId, UUID servicePersonId);

    @Query("select d.orderId from ServiceOrderProviderDecline d where d.servicePersonId = :servicePersonId")
    List<Long> findOrderIdsByServicePersonId(@Param("servicePersonId") UUID servicePersonId);
}

