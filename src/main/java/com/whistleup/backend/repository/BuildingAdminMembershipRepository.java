package com.whistleup.backend.repository;

import com.whistleup.backend.entity.BuildingAdminMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingAdminMembershipRepository extends JpaRepository<BuildingAdminMembership, Long> {

    @Query("SELECT m FROM BuildingAdminMembership m JOIN FETCH m.building WHERE TRIM(m.adminPhone) = :phone")
    List<BuildingAdminMembership> findByAdminPhoneTrimmed(@Param("phone") String phone);
}
