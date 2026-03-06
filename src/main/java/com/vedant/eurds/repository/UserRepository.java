package com.vedant.eurds.repository;

import com.vedant.eurds.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttemptCount = u.failedAttemptCount + 1 WHERE u.username = :username")
    void incrementFailedAttempts(@Param("username") String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLocked = true WHERE u.username = :username")
    void lockAccount(@Param("username") String username);

}