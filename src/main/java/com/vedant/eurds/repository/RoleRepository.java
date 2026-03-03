package com.vedant.eurds.repository;

import com.vedant.eurds.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Spring auto-generates: SELECT * FROM roles WHERE role_name = ?
    Optional<Role> findByRoleName(String roleName);
}