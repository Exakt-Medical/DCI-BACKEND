package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleId(String roleId);
}
