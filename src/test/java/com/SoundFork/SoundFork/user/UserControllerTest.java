package com.SoundFork.SoundFork.user;

import com.SoundFork.SoundFork.user.dto.CreateUserRequest;
import com.SoundFork.SoundFork.user.dto.UserResponse;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.SoundFork.SoundFork.user.controller.UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.SoundFork.SoundFork.auth.JwtUtil jwtUtil;

    @Test
    @WithMockUser
    @DisplayName("GET /users/{id} — 200 with user data")
    void getById_success() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .role("USER")
                .avatarUrl("/users/1/avatar/test.jpg")
                .build();

        when(userService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users — 400 on validation error")
    void create_validationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ab");
        request.setEmail("invalid");
        request.setPassword("12");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }
}
