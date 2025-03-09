package team.snapnestspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.snapnestspring.model.Album;
import team.snapnestspring.model.AlbumUserRole;

import java.util.List;
import java.util.Optional;

public interface AlbumUserRoleRepository extends JpaRepository<AlbumUserRole, Long> {
    Optional<AlbumUserRole> findByAlbumIdAndUserId(Long albumId, Long id);
    List<AlbumUserRole> findByAlbumId(Long albumId);
    void deleteByAlbumId(Long albumId);
    void deleteByAlbum(Album album);
}
