package com.kuet.ums.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kuet.ums.entity.UserAccount;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AccountPrincipal implements UserDetails, CredentialsContainer {
    private final Long id;
    private final String email;
    private final List<SimpleGrantedAuthority> authorities;
    private String password;

    public AccountPrincipal(UserAccount account) {
        id = account.getId();
        email = account.getEmail();
        password = account.getPasswordHash();
        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    public Long getId() { return id; }
    @Override
    public String getUsername() { return email; }
    @Override
    @JsonIgnore
    public String getPassword() { return password; }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public void eraseCredentials() { password = null; }
}
