package team.snapnestspring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.snapnestspring.model.*;
import team.snapnestspring.repository.*;
import team.snapnestspring.service.EmailService;
import team.snapnestspring.service.PhotoService;
import team.snapnestspring.service.UserService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumUserRoleRepository albumRoleUserService;

    @Autowired
    private EmailService emailService;

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
            List<User> invitedUsers = new ArrayList<>();
            for (int i = 0; i < usernames.size(); i++) {
                Optional<User> userOptional = userRepository.findByUsername(usernames.get(i));
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    AlbumUserRole.Role role = AlbumUserRole.Role.valueOf(roles.get(i));
                    AlbumUserRole albumUserRole = new AlbumUserRole(jointAlbum, user, role);
                    albumUserRoleRepository.save(albumUserRole);
                    invitedUsers.add(user);
                } else {
                    model.addAttribute("error", "User '" + usernames.get(i) + "' not found.");
                    return "SnapNestTemplates/album/create_joint_album";
                }
            }
            emailService.sendAlbumInvitationEmails(jointAlbum, invitedUsers);
            return "redirect:/joint_albums";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating joint album: " + e.getMessage());
            return "SnapNestTemplates/album/create_joint_album";
        }
    }

    /*@GetMapping("/{albumId}")
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
    }*/

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
            List<AlbumUserRole> participants = albumUserRoleRepository.findByAlbumId(albumId);

            model.addAttribute("album", album);
            model.addAttribute("role", role);
            model.addAttribute("photos", photos);
            model.addAttribute("participants", participants);

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

    @GetMapping("/{albumId}/photo/{photoId}")
    public String viewPhoto(@PathVariable Long albumId,
                            @PathVariable Long photoId,
                            Model model) {
        try {
            User user = userService.getCurrentUser();
            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, user.getId());
            if (albumUserRole.isEmpty() ||
                    (albumUserRole.get().getRole() != AlbumUserRole.Role.OWNER &&
                            albumUserRole.get().getRole() != AlbumUserRole.Role.READ_ONLY &&
                            albumUserRole.get().getRole() != AlbumUserRole.Role.READ_WRITE)) {
                model.addAttribute("error", "You do not have permission to view this photo.");
                return "SnapNestTemplates/album/joint_album_details";
            }
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));
            model.addAttribute("role", albumUserRole.get().getRole());
            model.addAttribute("photo", photo);
            return "SnapNestTemplates/photo/joint_photo_view";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading photo: " + e.getMessage());
            return "SnapNestTemplates/error";
        }
    }

    @PostMapping("/{albumId}/delete_photo/{photoId}")
    public String deletePhotoFromJointAlbum(@PathVariable Long albumId,
                                            @PathVariable Long photoId,
                                            Principal principal) {
        String username = principal.getName();
        photoService.deletePhotoFromJointAlbum(albumId, photoId, username);
        return "redirect:/joint_albums/" + albumId;
    }

    @PostMapping("/{albumId}/edit_photo/{photoId}")
    public String editPhoto(@PathVariable Long albumId, @PathVariable Long photoId,
                            @RequestParam String newName, Principal principal) {
        String username = principal.getName();
        photoService.editPhotoInJointAlbum(albumId, photoId, newName, username);
        return "redirect:/joint_albums/" + albumId;
    }

    @GetMapping("/{albumId}/edit")
    public String showEditAlbumForm(@PathVariable Long albumId, Model model) {
        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            Album album = optionalAlbum.get();
            User currentUser = userService.getCurrentUser();

            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, currentUser.getId());
            if (albumUserRole.isPresent() && albumUserRole.get().getRole() == AlbumUserRole.Role.OWNER) {
                List<AlbumUserRole> albumUserRoles = albumUserRoleRepository.findByAlbumId(albumId);
                model.addAttribute("album", album);
                model.addAttribute("albumUserRoles", albumUserRoles);
                return "SnapNestTemplates/album/edit_joint_album";
            } else {
                model.addAttribute("error", "You do not have permission to edit this album.");
                return "redirect:/joint_albums";
            }
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }

    @PostMapping("/{albumId}/edit")
    public String editAlbum(@PathVariable Long albumId,
                            @RequestParam String albumName,
                            Model model) {
        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            Album album = optionalAlbum.get();
            User currentUser = userService.getCurrentUser();

            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, currentUser.getId());
            if (albumUserRole.isPresent() && albumUserRole.get().getRole() == AlbumUserRole.Role.OWNER) {
                album.setName(albumName);
                albumRepository.save(album);

                return "redirect:/joint_albums/" + albumId;
            } else {
                model.addAttribute("error", "You do not have permission to edit this album.");
                return "redirect:/joint_albums";
            }
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }

    @GetMapping("/{albumId}/add_users")
    public String showAddUsersForm(@PathVariable Long albumId, Model model) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isPresent()) {
            User currentUser = userService.getCurrentUser();
            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, currentUser.getId());
            if (albumUserRole.isPresent() && albumUserRole.get().getRole() == AlbumUserRole.Role.OWNER) {
                model.addAttribute("album", album.get());
                return "SnapNestTemplates/album/add_users";
            } else {
                model.addAttribute("error", "You do not have permission to add users to this album.");
                return "redirect:/joint_albums";
            }
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }

    @PostMapping("/{albumId}/add_users")
    public String addUsersToAlbum(@PathVariable Long albumId,
                                  @RequestParam List<String> usernames,
                                  @RequestParam List<String> roles,
                                  Model model) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isPresent()) {
            List<String> notFoundUsers = new ArrayList<>();
            List<User> addedUsers = new ArrayList<>();

            for (String username : usernames) {
                Optional<User> userOptional = userRepository.findByUsername(username);
                if (userOptional.isEmpty()) {
                    notFoundUsers.add(username);
                } else {
                    addedUsers.add(userOptional.get());
                }
            }

            if (!notFoundUsers.isEmpty()) {
                model.addAttribute("error", "Users not found: " + String.join(", ", notFoundUsers));
                model.addAttribute("album", album.get());
                return "SnapNestTemplates/album/add_users";
            }

            for (int i = 0; i < usernames.size(); i++) {
                Optional<User> userOptional = userRepository.findByUsername(usernames.get(i));
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    AlbumUserRole.Role role = AlbumUserRole.Role.valueOf(roles.get(i));
                    AlbumUserRole albumUserRole = new AlbumUserRole(album.get(), user, role);
                    albumUserRoleRepository.save(albumUserRole);
                }
            }

            emailService.sendAlbumInvitationEmails(album.get(), addedUsers);

            return "redirect:/joint_albums/" + albumId;
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }


    @GetMapping("/{albumId}/edit_roles")
    public String showEditRolesForm(@PathVariable Long albumId, Model model) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isPresent()) {
            User currentUser = userService.getCurrentUser();
            Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, currentUser.getId());
            if (albumUserRole.isPresent() && albumUserRole.get().getRole() == AlbumUserRole.Role.OWNER) {
                model.addAttribute("album", album.get());

                List<AlbumUserRole> albumUserRoles = albumUserRoleRepository.findByAlbumId(albumId).stream()
                        .filter(role -> role.getRole() != AlbumUserRole.Role.OWNER)
                        .collect(Collectors.toList());

                model.addAttribute("albumUserRoles", albumUserRoles);
                return "SnapNestTemplates/album/edit_roles";
            } else {
                model.addAttribute("error", "You do not have permission to edit roles.");
                return "redirect:/joint_albums";
            }
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }


    @PostMapping("/{albumId}/edit_roles")
    public String updateRoles(@PathVariable Long albumId,
                              @RequestParam Map<String, String> userRoles,
                              Model model) {
        Optional<Album> album = albumRepository.findById(albumId);
        if (album.isPresent()) {
            for (Map.Entry<String, String> entry : userRoles.entrySet()) {
                try {
                    Long userId = Long.parseLong(entry.getKey().replace("userRoles[", "").replace("]", ""));
                    Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, userId);
                    if (albumUserRole.isPresent()) {
                        albumUserRole.get().setRole(AlbumUserRole.Role.valueOf(entry.getValue()));
                        albumUserRoleRepository.save(albumUserRole.get());
                    }
                } catch (NumberFormatException e) {
                    model.addAttribute("error", "Invalid user ID");
                    return "redirect:/joint_albums";
                }
            }

            return "redirect:/joint_albums/" + albumId + "/edit";
        } else {
            model.addAttribute("error", "Album not found");
            return "redirect:/joint_albums";
        }
    }

    @PostMapping("/{albumId}/delete/{userId}")
    public String removeUserFromAlbum(@PathVariable Long albumId, @PathVariable Long userId, Model model) {
        Optional<AlbumUserRole> albumUserRole = albumUserRoleRepository.findByAlbumIdAndUserId(albumId, userId);
        if (albumUserRole.isPresent()) {
            albumUserRoleRepository.delete(albumUserRole.get());
            model.addAttribute("success", "User removed successfully.");
        } else {
            model.addAttribute("error", "User not found in this album.");
        }
        return "redirect:/joint_albums/" + albumId + "/edit";
    }

    @Transactional
    @PostMapping("/{albumId}/delete")
    public String deleteAlbum(@PathVariable Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("Album not found"));

        albumUserRoleRepository.deleteByAlbum(album);
        albumRepository.delete(album);

        return "redirect:/joint_albums";
    }

    @PostMapping("/check_users")
    @ResponseBody
    public ResponseEntity<?> checkUsers(@RequestBody List<String> usernames) {
        User currentUser = userService.getCurrentUser();

        if (usernames.contains(currentUser.getUsername())) {
            return ResponseEntity.badRequest().body("You cannot add your own username.");
        }

        List<String> notFoundUsers = usernames.stream()
                .filter(username -> userRepository.findByUsername(username).isEmpty())
                .collect(Collectors.toList());

        return ResponseEntity.ok(notFoundUsers);
    }
}
