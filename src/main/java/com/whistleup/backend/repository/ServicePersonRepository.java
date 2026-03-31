package com.whistleup.backend.repository;

import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.entity.ServicePerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePersonRepository extends JpaRepository<ServicePerson, UUID> {

    List<ServicePerson> findAllByIsActive(Boolean isActive);

    // Fetch all active service persons who handle a given service type
    @Query("SELECT sp FROM ServicePerson sp JOIN sp.serviceTypes st WHERE st = :serviceType AND sp.isActive = true")
    List<ServicePerson> findActiveByServiceType(@Param("serviceType") ServiceOrderType serviceType);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<ServicePerson> findByPhoneNumber(String phoneNumber);
}