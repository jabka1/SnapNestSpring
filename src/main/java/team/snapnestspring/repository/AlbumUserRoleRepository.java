package team.snapnestspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.snapnestspring.model.AlbumUserRole;

import java.util.Optional;

public interface AlbumUserRoleRepository extends JpaRepository<AlbumUserRole, Long> {
    Optional<AlbumUserRole> findByAlbumIdAndUserId(Long albumId, Long id);
}
