package com.kopal.smartknowledgebase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopal.smartknowledgebase.config.SecurityConfig;
import com.kopal.smartknowledgebase.dto.AuthResponse;
import com.kopal.smartknowledgebase.dto.LoginRequest;
import com.kopal.smartknowledgebase.dto.RegisterRequest;
import com.kopal.smartknowledgebase.dto.UserResponse;
import com.kopal.smartknowledgebase.exception.DuplicateEmailException;
import com.kopal.smartknowledgebase.exception.InvalidCredentialsException;
import com.kopal.smartknowledgebase.security.CustomUserDetailsService;
import com.kopal.smartknowledgebase.security.JsonAccessDeniedHandler;
import com.kopal.smartknowledgebase.security.JsonAuthenticationEntryPoint;
import com.kopal.smartknowledgebase.security.JwtAuthenticationFilter;
import com.kopal.smartknowledgebase.security.JwtService;
import com.kopal.smartknowledgebase.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerReturns201OnSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Kopal");
        request.setEmail("kopal@example.com");
        request.setPassword("password123");

        UserResponse response = new UserResponse(1L, "Kopal", "kopal@example.com", LocalDateTime.now());
        when(authenticationService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("kopal@example.com"));
    }

    @Test
    void registerReturns409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Kopal");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(authenticationService.register(any()))
                .thenThrow(new DuplicateEmailException("existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerReturns400WhenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Kopal");
        request.setEmail("kopal@example.com");
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns200OnSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("kopal@example.com");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse("jwt-token", 1L, "Kopal", "kopal@example.com");
        when(authenticationService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void loginReturns401OnInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("kopal@example.com");
        request.setPassword("wrong-password");

        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("kopal@example.com");
        request.setPassword("password123");

        when(authenticationService.login(any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "Kopal", "kopal@example.com"));

        // No .with(user(...)) applied here — proves /api/auth/** is genuinely permitAll()
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}