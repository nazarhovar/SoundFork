package com.SoundFork.SoundFork.user.controller;

import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.common.util.ImageUtils;
import com.SoundFork.SoundFork.user.dto.CreateUserRequest;
import com.SoundFork.SoundFork.user.dto.UserResponse;
import com.SoundFork.SoundFork.user.dto.UserUpdateRequest;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @PostMapping("/{id}/avatar")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                     Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User " + authentication.getName() + " not found"));
        if (!currentUser.getId().equals(id) && currentUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the user or an admin can upload an avatar");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        Path avatarDir = Paths.get(uploadDir, "avatars", String.valueOf(id));
        String savedPath = ImageUtils.saveAvatar(file, avatarDir);
        user.setAvatarPath(savedPath);
        userRepository.save(user);

        return userService.getById(id);
    }

    @GetMapping("/{id}/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id, @PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path baseDir = Paths.get(uploadDir, "avatars", String.valueOf(id)).normalize();
        Path filePath = baseDir.resolve(filename).normalize();
        if (!filePath.startsWith(baseDir) || !filePath.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(filePath);
        String contentType = "image/" + filename.substring(filename.lastIndexOf('.') + 1);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
    }
}
