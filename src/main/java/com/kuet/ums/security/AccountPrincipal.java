package com.kuet.ums.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AccountPrincipal implements UserDetails, CredentialsContainer {

    private final Long id;
    private final Role role;
    private final String email;
    private final List<SimpleGrantedAuthority> authorities;
    private String password;

    public AccountPrincipal(UserAccount account) {
        id = account.getId();
        role = account.getRole();
        email = account.getEmail();
        password = account.getPasswordHash();
        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
