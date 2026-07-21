package com.taskflow.auth.repository;
import com.taskflow.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUsername(String username);
    Optional<UserCredential> findByEmail(String email);
    boolean existsByEmail(String email);

    List<UserCredential> findByRole(String role);
    List<UserCredential> findByRoleAndStatus(String role, String status);
    List<UserCredential> findByRoleAndStatusAndDepartment(String role, String status, String department);
}
