package com.whistleup.backend.repository;

import com.whistleup.backend.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    List<RentPayment> findByProfileIdOrderByRentYearDescRentMonthDesc(String profileId);

    Optional<RentPayment> findByProfileIdAndBuildingIdAndRentYearAndRentMonth(
            String profileId,
            String buildingId,
            Integer rentYear,
            Integer rentMonth
    );

    void deleteByProfileId(String profileId);
}
