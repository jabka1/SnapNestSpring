package team.snapnestspring.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.snapnestspring.model.Album;
import team.snapnestspring.model.Photo;
import team.snapnestspring.model.User;
import team.snapnestspring.repository.AlbumRepository;
import team.snapnestspring.repository.PhotoRepository;
import team.snapnestspring.service.PhotoService;
import team.snapnestspring.service.UserService;

import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @GetMapping("/mainPage")
    public String mainPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        model.addAttribute("isAuthenticated", isAuthenticated);
        return "SnapNestTemplates/mainPage";
    }

    @GetMapping("/albums")
    public String showMainAlbum(Model model) {
        User user = userService.getCurrentUser();
        List<Album> mainAlbums = albumRepository.findByUserIdAndParentAlbumIsNullAndIsJointFalse(user.getId());
        List<Photo> photos = photoRepository.findByUserIdAndAlbumIsNull(user.getId());

        String uploadUrl = "/albums/upload";

        model.addAttribute("rootAlbum", true);
        model.addAttribute("albums", mainAlbums);
        model.addAttribute("photos", photos);
        model.addAttribute("uploadUrl", uploadUrl);
        return "SnapNestTemplates/album/albums";
    }

    /*@GetMapping("/albums/{albumId}")
    public String viewAlbum(@PathVariable Long albumId, Model model, Principal principal) {
        User user = userService.getCurrentUser();
        Optional<Album> albumOpt = albumRepository.findByIdAndUserIdAndIsJointFalse(albumId, user.getId());

        if (albumOpt.isEmpty()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        String uploadUrl = "/albums/" + album.getId() + "/upload";

        boolean isOwner = album.getUser().getUsername().equals(principal.getName());
        String sharedUrl = null;

        if (isOwner) {
            String encodedId = Base64.getUrlEncoder().encodeToString(albumId.toString().getBytes());
            sharedUrl = "http://localhost:8080/shared/album/" + encodedId;
        }

        model.addAttribute("album", album);
        model.addAttribute("albums", album.getSubAlbums());
        model.addAttribute("photos", album.getPhotos());
        model.addAttribute("parentAlbumId", albumId);
        model.addAttribute("uploadUrl", uploadUrl);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("sharedUrl", sharedUrl);

        return "SnapNestTemplates/album/albums";
    }*/

    @GetMapping("/albums/{albumId}")
    public String viewAlbum(@PathVariable Long albumId, Model model, Principal principal, HttpServletRequest request) {
        User user = userService.getCurrentUser();
        Optional<Album> albumOpt = albumRepository.findByIdAndUserIdAndIsJointFalse(albumId, user.getId());

        if (albumOpt.isEmpty()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        String uploadUrl = "/albums/" + album.getId() + "/upload";

        boolean isOwner = album.getUser().getUsername().equals(principal.getName());
        String sharedUrl = null;

        if (isOwner) {
            String encodedId = Base64.getUrlEncoder().encodeToString(albumId.toString().getBytes());

            String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");

            sharedUrl = baseUrl + "/shared/album/" + encodedId;
        }

        model.addAttribute("album", album);
        model.addAttribute("albums", album.getSubAlbums());
        model.addAttribute("photos", album.getPhotos());
        model.addAttribute("parentAlbumId", albumId);
        model.addAttribute("uploadUrl", uploadUrl);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("sharedUrl", sharedUrl);

        return "SnapNestTemplates/album/albums";
    }



    @GetMapping("/shared/album/{encodedId}")
    public String sharedAlbum(@PathVariable String encodedId, Model model) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedId);
            Long albumId = Long.parseLong(new String(decodedBytes));

            Album album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found"));

            List<Photo> publicPhotos = photoRepository.findByAlbumAndIsPublicTrue(album);

            if (publicPhotos.isEmpty()) {
                return "SnapNestTemplates/error";
            }

            model.addAttribute("album", album);
            model.addAttribute("photos", publicPhotos);
            model.addAttribute("owner", album.getUser().getUsername());

            return "SnapNestTemplates/album/shared_album_view";
        } catch (Exception e) {
            return "SnapNestTemplates/error";
        }
    }

    /*@GetMapping("/photo/{photoId}")
    public String viewPhoto(@PathVariable Long photoId, Model model, Principal principal) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        boolean isOwner = photo.getUser().getUsername().equals(principal.getName());
        String sharedUrl = null;

        if(isOwner){
            model.addAttribute("photo", photo);
            model.addAttribute("isPublic", photo.isPublic());
            model.addAttribute("isOwner", isOwner);
            if (photo.isPublic()) {
                String encodedId = Base64.getUrlEncoder().encodeToString(photoId.toString().getBytes());
                sharedUrl = "http://localhost:8080/shared/photo/" + encodedId;
                model.addAttribute("sharedUrl", sharedUrl);
            }
            return "SnapNestTemplates/photo/photo_view";
        }
        return "SnapNestTemplates/error";
    }*/

    @GetMapping("/photo/{photoId}")
    public String viewPhoto(@PathVariable Long photoId, Model model, Principal principal, HttpServletRequest request) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        boolean isOwner = photo.getUser().getUsername().equals(principal.getName());
        String sharedUrl = null;

        if (isOwner) {
            model.addAttribute("photo", photo);
            model.addAttribute("isPublic", photo.isPublic());
            model.addAttribute("isOwner", isOwner);
            if (photo.isPublic()) {
                String encodedId = Base64.getUrlEncoder().encodeToString(photoId.toString().getBytes());
                String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
                sharedUrl = baseUrl + "/shared/photo/" + encodedId;
                model.addAttribute("sharedUrl", sharedUrl);
            }
            return "SnapNestTemplates/photo/photo_view";
        }
        return "SnapNestTemplates/error";
    }


    @GetMapping("/shared/photo/{encodedId}")
    public String sharedPhoto(@PathVariable String encodedId, Model model) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedId);
            Long photoId = Long.parseLong(new String(decodedBytes));

            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));

            if (!photo.isPublic()) {
                return "SnapNestTemplates/error";
            }

            model.addAttribute("photo", photo);
            model.addAttribute("owner", photo.getUser().getUsername());
            return "SnapNestTemplates/photo/shared_photo_view";
        } catch (Exception e) {
            return "SnapNestTemplates/error";
        }
    }


    @GetMapping("/createAlbum")
    public String showCreateAlbumForm(@RequestParam(required = false) Long parentAlbumId, Model model) {
        model.addAttribute("parentAlbumId", parentAlbumId);
        return "SnapNestTemplates/album/create_album";
    }

    @PostMapping("/albums/{albumId}/upload")
    public String uploadPhotoInAlbum(@PathVariable Long albumId,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
                                     Model model) {
        try {
            User user = userService.getCurrentUser();

            Optional<Album> albumOpt = albumRepository.findByIdAndUserId(albumId, user.getId());
            if (albumOpt.isEmpty()) {
                model.addAttribute("error", "Album not found or access denied.");
                return "SnapNestTemplates/album/albums";
            }

            Album album = albumOpt.get();
            photoService.uploadPhoto(file, isPublic, album.getId(), user.getUsername());

            return "redirect:/albums/" + albumId;
        } catch (Exception e) {
            model.addAttribute("error", "Photo upload error: " + e.getMessage());
            return "SnapNestTemplates/album/albums";
        }
    }

    @PostMapping("/albums/upload")
    public String uploadPhotoRoot(@RequestParam("file") MultipartFile file,
                                  @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
                                  Model model) {
        photoRepository.makeAlbumIdNullable();
        try {
            User user = userService.getCurrentUser();
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload.");
                return "SnapNestTemplates/album/albums";
            }
            photoService.uploadPhoto(file, isPublic, null, user.getUsername());
            return "redirect:/albums";
        } catch (Exception e) {
            model.addAttribute("error", "Photo upload error: " + e.getMessage());
            return "SnapNestTemplates/album/albums";
        }
    }

    @PostMapping("/createAlbum")
    public String createAlbum(@RequestParam String albumName,
                              @RequestParam(required = false) Long parentAlbumId,
                              Model model) {
        try {
            User user = userService.getCurrentUser();

            Album newAlbum = new Album(albumName, user);

            if (parentAlbumId != null) {
                Optional<Album> parentAlbumOpt = albumRepository.findByIdAndUserId(parentAlbumId, user.getId());
                if (parentAlbumOpt.isPresent()) {
                    newAlbum.setParentAlbum(parentAlbumOpt.get());
                }
            }

            albumRepository.save(newAlbum);

            return parentAlbumId != null ? "redirect:/albums/" + parentAlbumId : "redirect:/albums";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "SnapNestTemplates/album/albums";
        }
    }

    @PostMapping("/photo/{photoId}/delete")
    public String deletePhoto(@PathVariable Long photoId, Principal principal) {
        User user = userService.getCurrentUser();
        photoService.deletePhoto(photoId, user.getUsername());
        return "redirect:/albums";
    }

    @PostMapping("/photo/{photoId}/edit")
    public String editPhoto(@PathVariable Long photoId,
                            @RequestParam String newName,
                            @RequestParam(name = "isPublic", defaultValue = "false") boolean isPublic,
                            Principal principal) {
        User user = userService.getCurrentUser();
        photoService.editPhoto(photoId, newName, isPublic, user.getUsername());
        return "redirect:/photo/" + photoId;
    }

    @GetMapping("/albums/{albumId}/edit")
    public String editAlbum(@PathVariable Long albumId, Model model, Principal principal) {
        User user = userService.getCurrentUser();
        Optional<Album> albumOpt = albumRepository.findByIdAndUserId(albumId, user.getId());

        if (albumOpt.isEmpty()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        model.addAttribute("album", album);
        return "SnapNestTemplates/album/edit_album";
    }

    @PostMapping("/albums/{albumId}/edit")
    public String updateAlbum(@PathVariable Long albumId,
                              @RequestParam String name,
                              Principal principal) {
        User user = userService.getCurrentUser();
        Optional<Album> albumOpt = albumRepository.findByIdAndUserId(albumId, user.getId());

        if (albumOpt.isEmpty()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        album.setName(name);
        albumRepository.save(album);

        return "redirect:/albums/" + albumId;
    }

    @PostMapping("/albums/{albumId}/delete")
    public String deleteAlbum(@PathVariable Long albumId, Principal principal) {
        User user = userService.getCurrentUser();
        Optional<Album> albumOpt = albumRepository.findByIdAndUserId(albumId, user.getId());

        if (albumOpt.isEmpty()) {
            return "redirect:/albums";
        }

        Album album = albumOpt.get();
        photoService.deleteAlbum(album);

        return "redirect:/albums";
    }

}
