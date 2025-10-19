package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {
    Optional<Users> findByPhone(String username);
    Optional<Users> findByEmail(String email);
}
