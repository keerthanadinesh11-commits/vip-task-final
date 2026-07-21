package com.taskflow.userservice.repository;

import com.taskflow.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByUsername(String username);

    boolean existsByEmail(String email);

    List<UserProfile> findByRole(String role);

    List<UserProfile> findByRoleAndStatusAndDepartment(String role, String status, String department);

    /** Direct reports of a manager — used to scope the Users page for MANAGER role. */
    List<UserProfile> findByManagerEmail(String managerEmail);
}
