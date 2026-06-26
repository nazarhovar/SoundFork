package com.SoundFork.SoundFork.user.service;

import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.user.dto.CreateUserRequest;
import com.SoundFork.SoundFork.user.dto.UserResponse;
import com.SoundFork.SoundFork.user.dto.UserUpdateRequest;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCleanupService userCleanupService;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        log.info("User created: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return buildUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
        log.debug("User fetched: id={}", id);

        return buildUserResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getSocialLinks() != null) {
            user.setSocialLinks(request.getSocialLinks());
        }

        log.info("User updated: id={}", id);

        return buildUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        userCleanupService.deleteUserWithData(user);
        userRepository.deleteById(id);

        log.info("User id={} deleted", id);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::buildUserResponse)
                .toList();
    }

    private UserResponse buildUserResponse(User user) {
        String avatarUrl = null;
        if (user.getAvatarPath() != null) {
            String[] parts = user.getAvatarPath().split("/");
            String filename = parts[parts.length - 1];
            avatarUrl = "/users/" + user.getId() + "/avatar/" + filename;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .bio(user.getBio())
                .avatarPath(user.getAvatarPath())
                .avatarUrl(avatarUrl)
                .socialLinks(user.getSocialLinks())
                .build();
    }
}
