package com.dci.clearance.repository;

import com.dci.clearance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRole(User.UserRole role);


    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") User.UserRole role);
}