package com.whistleup.backend.repository;

import com.whistleup.backend.entity.UserPushTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPushTokenRepository
        extends JpaRepository<UserPushTokenEntity, String> {
}
