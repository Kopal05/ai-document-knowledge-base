package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.AuthResponse;
import com.kopal.smartknowledgebase.dto.LoginRequest;
import com.kopal.smartknowledgebase.dto.RegisterRequest;
import com.kopal.smartknowledgebase.dto.UserResponse;
import com.kopal.smartknowledgebase.entity.User;
import com.kopal.smartknowledgebase.exception.DuplicateEmailException;
import com.kopal.smartknowledgebase.exception.InvalidCredentialsException;
import com.kopal.smartknowledgebase.repository.UserRepository;
import com.kopal.smartknowledgebase.security.CustomUserDetails;
import com.kopal.smartknowledgebase.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerHashesPasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Kopal");
        request.setEmail("kopal@example.com");
        request.setPassword("plaintext123");

        when(userRepository.existsByEmail("kopal@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = authenticationService.register(request);

        assertThat(response.getEmail()).isEqualTo("kopal@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerThrowsDuplicateEmailExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Name");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenOnSuccessfulAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kopal@example.com");
        request.setPassword("correct-password");

        User user = new User();
        user.setId(1L);
        user.setName("Kopal");
        user.setEmail("kopal@example.com");
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authenticationService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("kopal@example.com");
    }

    @Test
    void loginThrowsInvalidCredentialsForUnknownEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginThrowsSameInvalidCredentialsExceptionForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("kopal@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}