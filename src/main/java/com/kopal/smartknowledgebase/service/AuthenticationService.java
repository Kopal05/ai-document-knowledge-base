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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        CustomUserDetails userDetails;
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            userDetails = (CustomUserDetails) authentication.getPrincipal();
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, userDetails.getId(), userDetails.getName(), userDetails.getUsername());
    }
}