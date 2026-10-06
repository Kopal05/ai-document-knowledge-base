package com.kopal.smartknowledgebase.security;

import com.kopal.smartknowledgebase.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implements Spring Security's UserDetailsService — the one method
 * (loadUserByUsername) the framework calls, by contract, whenever it
 * needs to look up a principal: once during login (via
 * AuthenticationManager/DaoAuthenticationProvider) and once per request
 * inside JwtAuthenticationFilter (to confirm the token's email still
 * maps to a real, current user).
 *
 * "username" here is the user's EMAIL — Spring Security's interface
 * uses generic "username" terminology, but nothing requires it to
 * actually be a username; using email directly, consistently, in both
 * call sites avoids introducing a separate "username" concept this
 * project has no other use for.
 *
 * UsernameNotFoundException is a Spring Security exception, not ours —
 * thrown here, it's caught inside AuthenticationService's login() and
 * converted to the generic InvalidCredentialsException before it ever
 * reaches a client (see AuthenticationService for why).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    }
}