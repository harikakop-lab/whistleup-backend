package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByPhone(String phone);

    @Query("SELECT p FROM Profile p WHERE p.email = :loginId OR p.phone = :loginId")
    Optional<Profile> findByEmailOrPhone(@Param("loginId") String loginId);
}
