package com.kopal.smartknowledgebase.config;

import com.kopal.smartknowledgebase.security.CustomUserDetailsService;
import com.kopal.smartknowledgebase.security.JsonAccessDeniedHandler;
import com.kopal.smartknowledgebase.security.JsonAuthenticationEntryPoint;
import com.kopal.smartknowledgebase.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration. Uses the current (Spring
 * Security 6 / Spring Boot 3.3) lambda-DSL style throughout —
 * WebSecurityConfigurerAdapter is deprecated/removed, deliberately not
 * used here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final String corsAllowedOrigin;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JsonAuthenticationEntryPoint authenticationEntryPoint,
                          JsonAccessDeniedHandler accessDeniedHandler,
                          @Value("${cors.allowed-origin:http://localhost:3000}") String corsAllowedOrigin) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protects against a browser automatically attaching
                // SESSION COOKIES to a forged cross-site request. This API
                // uses stateless bearer tokens instead — a forged request
                // from another site has no way to obtain or attach a
                // caller's JWT, so there's nothing for CSRF protection to
                // defend against here. (CSRF would matter again the
                // moment this API switched to cookie-based sessions.)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS: Spring Security creates no HttpSession and
                // never relies on one — every request re-proves identity
                // via its own JWT. This is what "stateless authentication"
                // means mechanically, not just as a buzzword.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Runs JwtAuthenticationFilter BEFORE Spring Security's
                // own username/password filter, since this API never
                // uses that filter at all (login is a plain @RestController
                // endpoint, not Spring Security's form-login flow) — this
                // just places our filter at a well-known, early point in
                // the standard chain.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: a slow-by-design hashing algorithm (unlike SHA-256,
        // which is fast — a property that's GOOD for checksums and BAD
        // for passwords, since fast hashing makes brute-forcing leaked
        // hashes cheap). BCrypt also auto-generates and embeds a random
        // salt per password, so two identical passwords never produce
        // the same stored hash.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // The standard Spring Security 6 way to obtain an
        // AuthenticationManager bean — WebSecurityConfigurerAdapter's old
        // authenticationManagerBean() override is gone; this is its
        // replacement.
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Deliberately NOT "*": an allow-all origin combined with
        // allowCredentials(true) is both invalid per the CORS spec and a
        // real security hole if it ever worked — it would let ANY
        // website's JavaScript make authenticated requests on a logged-in
        // user's behalf. corsAllowedOrigin is externalized specifically
        // so the eventual React frontend's real origin can be configured
        // per-environment without touching this code.
        configuration.setAllowedOrigins(List.of(corsAllowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}