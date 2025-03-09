package team.snapnestspring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.snapnestspring.model.*;
import team.snapnestspring.repository.*;
import team.snapnestspring.service.PhotoService;
import team.snapnestspring.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/joint_albums")
public class JointAlbumController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private AlbumUserRoleRepository albumUserRoleRepository;

    @Autowired
    private PhotoService photoService;

    @GetMapping
    public String viewJointAlbums(Model model) {
        User currentUser = userService.getCurrentUser();

        List<Album> jointAlbums = albumRepository.findAllAlbumsForUser(currentUser.getId());
        model.addAttribute("jointAlbums", jointAlbums);

        return "SnapNestTemplates/album/joint_albums";
    }


    @GetMapping("/create")
    public String showCreateJointAlbumForm(Model model) {
        return "SnapNestTemplates/album/create_joint_album";
    }

    @PostMapping("/create")
    public String createJointAlbum(@RequestParam String albumName,
                                   @RequestParam List<String> usernames,
                                   @RequestParam List<String> roles,
                                   Model model) {
        try {
            User currentUser = userService.getCurrentUser();
            Album jointAlbum = new Album(albumName, currentUser);
            jointAlbum.setJoint(true);
            albumRepository.save(jointAlbum);
            AlbumUserRole albumUserRoleOwner = new AlbumUserRole(jointAlbum, currentUser, AlbumUserRole.Role.OWNER);
            albumUserRoleRepository.save(albumUserRoleOwner);
            for (int i = 0; i < usernames.size(); i++) {
                Optional<User> userOptional = userRepository.findByUsername(usernames.get(i));
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    AlbumUserRole.Role role = AlbumUserRole.Role.valueOf(roles.get(i));
                    AlbumUserRole albumUserRole = new AlbumUserRole(jointAlbum, user, role);
                    albumUserRoleRepository.save(albumUserRole);
                } else {
                    model.addAttribute("error", "User '" + usernames.get(i) + "' not found.");
                    return "SnapNestTemplates/album/create_joint_album";
                }
            }
            return "redirect:/joint_albums";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating joint album: " + e.getMessage());
            return "SnapNestTemplates/album/create_joint_album";
        }
    }

    @GetMapping("/{albumId}")
    public String showAlbumDetails(@PathVariable Long albumId, Model model) {
        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            Album album = optionalAlbum.get();
            User currentUser = userService.getCurrentUser();

            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, currentUser.getId());

            String role = "NONE";
            if (albumUserRole.isPresent()) {
                role = albumUserRole.get().getRole().name();
            }

            List<Photo> photos = photoService.getPhotosByAlbumId(albumId);

            model.addAttribute("album", album);
            model.addAttribute("role", role);
            model.addAttribute("photos", photos);

            return "SnapNestTemplates/album/joint_album_details";
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }

    @PostMapping("/{albumId}/upload")
    public String uploadPhotoJoint(@RequestParam("file") MultipartFile file,
                                   @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
                                   @PathVariable Long albumId,
                                   Model model) {
        try {
            User user = userService.getCurrentUser();

            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, user.getId());
            if (albumUserRole.isEmpty() ||
                    (albumUserRole.get().getRole() != AlbumUserRole.Role.OWNER &&
                            albumUserRole.get().getRole() != AlbumUserRole.Role.READ_WRITE)) {
                model.addAttribute("error", "You do not have permission to upload photos to this album.");
                return "SnapNestTemplates/album/joint_album_details";
            }

            Album album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found"));

            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload.");
                return "SnapNestTemplates/album/joint_album_details";
            }

            photoService.uploadPhotoToJointAlbum(file, isPublic, albumId, user.getUsername());
            return "redirect:/joint_albums/" + albumId;
        } catch (Exception e) {
            model.addAttribute("error", "Photo upload error: " + e.getMessage());
            return "SnapNestTemplates/album/joint_album_details";
        }
    }

    @PostMapping("/check_users")
    @ResponseBody
    public ResponseEntity<?> checkUsers(@RequestBody List<String> usernames) {
        List<String> notFoundUsers = usernames.stream()
                .filter(username -> userRepository.findByUsername(username).isEmpty())
                .collect(Collectors.toList());

        return ResponseEntity.ok(notFoundUsers);
    }
}
