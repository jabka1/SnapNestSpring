package team.snapnestspring.repository;

import team.snapnestspring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationtoken(String token);
    Optional<User> findByRecoveryToken(String recoveryToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
