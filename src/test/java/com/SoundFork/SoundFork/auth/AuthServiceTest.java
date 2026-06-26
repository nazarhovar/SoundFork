package com.SoundFork.SoundFork.auth;

import com.SoundFork.SoundFork.common.email.EmailService;
import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Register new user successfully — sends welcome email")
    void register_success_sendsEmail() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
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
        when(jwtUtil.generateToken("newuser", "USER")).thenReturn("token123");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("newuser", response.getUser().getUsername());

        verify(emailService).sendEmail(eq("new@test.com"), eq("Welcome to SoundFork"), anyString());
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new@test.com", userCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("Register with duplicate username throws CONFLICT")
    void register_duplicateUsername_throws() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new User()));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Register with duplicate email throws CONFLICT")
    void register_duplicateEmail_throws() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("used@test.com")).thenReturn(Optional.of(new User()));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("used@test.com");
        request.setPassword("password123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Login with valid credentials returns token")
    void login_success() {
        User user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken("testuser", "USER")).thenReturn("token456");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token456", response.getToken());
    }

    @Test
    @DisplayName("Login with wrong password throws UNAUTHORIZED")
    void login_wrongPassword_throws() {
        User user = User.builder()
                .username("testuser")
                .password("encoded")
                .role(Role.USER)
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
