package team.snapnestspring.repository;

import team.snapnestspring.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByIdAndUserUsername(Long id, String username);
    List<Album> findByUserIdAndParentAlbumIsNull(Long userId);
    Optional<Album> findByNameAndUserUsername(String name, String username);
    Optional<Album> findByIdAndUserId(Long albumId, Long id);
}