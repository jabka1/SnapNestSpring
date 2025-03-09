package team.snapnestspring.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.snapnestspring.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByIdAndUserUsername(Long id, String username);
    List<Album> findByUserIdAndParentAlbumIsNull(Long userId);
    Optional<Album> findByNameAndUserUsername(String name, String username);
    Optional<Album> findByIdAndUserId(Long albumId, Long id);

    @Query("SELECT a FROM Album a " +
            "JOIN AlbumUserRole aur ON a.id = aur.album.id " +
            "WHERE aur.user.id = :userId AND (aur.role = 'READ_ONLY' OR aur.role = 'READ_WRITE' OR aur.role = 'OWNER') AND a.isJoint = true")
    List<Album> findAllAlbumsForUser(Long userId);

    List<Album> findByUserIdAndParentAlbumIsNullAndIsJointFalse(Long id);
    Optional<Album> findByIdAndUserIdAndIsJointFalse(Long albumId, Long id);
    List<Album> findAllByUserIdAndIsJointTrue(Long id);
}