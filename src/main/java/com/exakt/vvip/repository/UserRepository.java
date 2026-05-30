package com.exakt.vvip.repository;

import com.exakt.vvip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByBranchId(Long branchId);
    List<User> findByManagerId(Long managerId);
    List<User> findByRole(User.UserRole role);

    // FIXED: Accept UserRole enum instead of String
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") User.UserRole role);
}