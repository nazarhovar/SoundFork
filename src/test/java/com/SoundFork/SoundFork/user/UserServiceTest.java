package com.SoundFork.SoundFork.user;

import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.user.dto.CreateUserRequest;
import com.SoundFork.SoundFork.user.dto.UserResponse;
import com.SoundFork.SoundFork.user.dto.UserUpdateRequest;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.user.service.UserCleanupService;
import com.SoundFork.SoundFork.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserCleanupService userCleanupService;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Create user successfully")
    void create_success() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .username(u.getUsername())
                    .email(u.getEmail())
                    .password(u.getPassword())
                    .role(u.getRole())
                    .build();
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("password123");

        UserResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());

        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded", userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("Get user by ID returns user response")
    void getById_success() {
        User user = User.builder().id(1L).username("testuser").email("test@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(1L);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    @DisplayName("Get user by ID throws when not found")
    void getById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    @DisplayName("Update user changes fields")
    void update_success() {
        User user = User.builder().id(1L).username("old").email("old@test.com").password("encoded").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("newencoded");

        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("newpass");

        UserResponse response = userService.update(1L, request);

        assertNotNull(response);
        assertEquals("newuser", user.getUsername());
        assertEquals("new@test.com", user.getEmail());
        assertEquals("newencoded", user.getPassword());
    }

    @Test
    @DisplayName("GetAll returns list of users")
    void getAll_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id(1L).username("user1").build(),
                User.builder().id(2L).username("user2").build()
        ));

        List<UserResponse> result = userService.getAll();

        assertEquals(2, result.size());
    }
}
