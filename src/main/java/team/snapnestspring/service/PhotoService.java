package team.snapnestspring.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team.snapnestspring.model.Album;
import team.snapnestspring.model.AlbumUserRole;
import team.snapnestspring.model.Photo;
import team.snapnestspring.model.User;
import team.snapnestspring.repository.AlbumRepository;
import team.snapnestspring.repository.AlbumUserRoleRepository;
import team.snapnestspring.repository.PhotoRepository;
import team.snapnestspring.repository.UserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoService {
    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Autowired
    private AlbumUserRoleRepository albumUserRoleRepository;

    @Transactional
    public void uploadPhoto(MultipartFile file, boolean isPublic, Long albumId, String username) throws IOException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        Album album = null;
        if (albumId != null) {
            album = albumRepository.findByIdAndUserUsername(albumId, username).orElseThrow(() -> new RuntimeException("Album not found"));
        }

        String fileName = file.getOriginalFilename();

        String newFileName = generateRandomFileName(fileName);
        String photoUrl = uploadToS3(file, newFileName);

        Photo photo = new Photo();
        photo.setName(fileName);
        photo.setUrl(photoUrl);
        photo.setPublic(isPublic);
        if (album != null) {
            photo.setAlbum(album);
        }
        photo.setUser(user);

        photoRepository.save(photo);
    }

    @Transactional
    public void uploadPhotoToJointAlbum(MultipartFile file, boolean isPublic, Long albumId, String username) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, user.getId());
        if (albumUserRole.isEmpty() || (albumUserRole.get().getRole() != AlbumUserRole.Role.OWNER &&
                albumUserRole.get().getRole() != AlbumUserRole.Role.READ_WRITE)) {
            throw new RuntimeException("You do not have permission to upload photos to this album.");
        }

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload.");
        }

        String fileName = file.getOriginalFilename();
        String newFileName = generateRandomFileName(fileName);
        String photoUrl = uploadToS3(file, newFileName);

        Photo photo = new Photo();
        photo.setName(fileName);
        photo.setUrl(photoUrl);
        photo.setPublic(isPublic);
        photo.setAlbum(album);
        photo.setUser(user);

        photoRepository.save(photo);
    }


    private String generateRandomFileName(String originalFileName) {
        String randomUUID = UUID.randomUUID().toString();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return randomUUID + fileExtension;
    }

    private String uploadToS3(MultipartFile file, String newFileName) throws IOException {
        String keyName = "photos/" + newFileName;
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            s3Client.putObject(bucketName, keyName, inputStream, metadata);
        }
        return s3Client.getUrl(bucketName, keyName).toString();
    }

    @Transactional
    public void deletePhoto(Long photoId, String username) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized action");
        }
        String key = photo.getUrl().substring(photo.getUrl().indexOf("photos/"));
        s3Client.deleteObject(bucketName, key);
        photoRepository.delete(photo);
    }

    @Transactional
    public void editPhoto(Long photoId, String newName, boolean isPublic, String username) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        if (!photo.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized action");
        }
        photo.setName(newName);
        photo.setPublic(isPublic);

        photoRepository.save(photo);
    }

    @Transactional
    public void deleteAlbum(Album album) {
        for (Photo photo : album.getPhotos()) {
            String key = photo.getUrl().substring(photo.getUrl().indexOf("photos/"));
            s3Client.deleteObject(bucketName, key);
            photoRepository.delete(photo);
        }
        for (Album subAlbum : album.getSubAlbums()) {
            deleteAlbum(subAlbum);
        }
        albumRepository.delete(album);
    }

    public List<Photo> getPhotosByAlbumId(Long albumId) {
        return photoRepository.findByAlbumId(albumId);
    }

}
