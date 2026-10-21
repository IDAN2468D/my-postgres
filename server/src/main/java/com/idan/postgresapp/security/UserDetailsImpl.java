package com.idan.postgresapp.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.idan.postgresapp.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;

    @JsonIgnore
    private String password;

    private String role;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String name, String email, String password, String role,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        String roleStr = (user.getRole() != null && !user.getRole().isBlank())
                ? user.getRole().toUpperCase()
                : "USER";

        if (!roleStr.startsWith("ROLE_")) {
            roleStr = "ROLE_" + roleStr;
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(roleStr);

        return new UserDetailsImpl(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
