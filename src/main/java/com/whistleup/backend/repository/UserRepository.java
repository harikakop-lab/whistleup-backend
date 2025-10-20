package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM Users u WHERE u.email = :loginId OR u.phoneNumber = :loginId")
    Optional<Users> findByEmailOrPhoneNumber(@Param("loginId") String loginId);
}
