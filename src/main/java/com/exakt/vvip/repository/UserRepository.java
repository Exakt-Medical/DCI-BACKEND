package com.exakt.vvip.repository;

import com.exakt.vvip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByBranchId(Long branchId);
    List<User> findByManagerId(Long managerId);
    List<User> findByRole(User.UserRole role);
}







