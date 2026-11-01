package com.kuet.ums.security;

import com.kuet.ums.repository.AccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    public AccountDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return accounts
            .findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .map(AccountPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }
}
