package team.snapnestspring.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import team.snapnestspring.model.Album;
import team.snapnestspring.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByAlbumId(Long albumId);
    List<Photo> findByUserIdAndAlbumIsNull(Long userId);
    List<Photo> findByAlbumAndIsPublicTrue(Album album);

    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE photo MODIFY album_id BIGINT NULL", nativeQuery = true)
    void makeAlbumIdNullable();
}