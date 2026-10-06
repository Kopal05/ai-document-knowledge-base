package com.kopal.smartknowledgebase.security;

import com.kopal.smartknowledgebase.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security's view of a User — a thin adapter, not a second copy
 * of User's data. See User's javadoc for why this wrapper exists instead
 * of User implementing UserDetails directly.
 *
 * getId() is the one addition beyond what UserDetails requires: every
 * document-ownership check in this project needs the user's numeric id,
 * not just their username (email). Controllers/services call
 * principal.getId() directly rather than re-querying UserRepository by
 * email just to get an id they already have in hand.
 *
 * No roles/authorities are implemented yet (getAuthorities() returns an
 * empty list) — this phase has exactly one kind of authenticated user,
 * with no admin/regular distinction. Adding roles later means extending
 * this one method, nothing structural changes.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getName() {
        return user.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}