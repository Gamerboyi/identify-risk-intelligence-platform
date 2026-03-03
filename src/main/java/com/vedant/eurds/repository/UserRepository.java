package com.vedant.eurds.repository;

import com.vedant.eurds.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring auto-generates: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Spring auto-generates: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Spring auto-generates: SELECT COUNT(*) FROM users WHERE username = ?
    boolean existsByUsername(String username);

    // Spring auto-generates: SELECT COUNT(*) FROM users WHERE email = ?
    boolean existsByEmail(String email);
}