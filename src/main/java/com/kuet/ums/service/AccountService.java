package com.kuet.ums.service;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;

    public AccountService(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    public List<AccountResponse> list(Role role) {
        return accounts.findAllByRoleOrderByIdAsc(role).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse get(Role role, Long id) {
        return AccountResponse.from(requireAccount(role, id));
    }

    @Transactional
    public AccountResponse create(Role role, CreateAccountRequest request) {
        if (accounts.existsByEmail(request.email())) throw duplicateEmail();
        UserAccount account = new UserAccount(request.fullName(), request.email(), passwords.encode(request.password()), role);
        return AccountResponse.from(accounts.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse update(Role role, Long id, UpdateAccountRequest request) {
        UserAccount account = requireAccount(role, id);
        if (accounts.existsByEmailAndIdNot(request.email(), id)) throw duplicateEmail();
        account.updateProfile(request.fullName(), request.email());
        if (request.password() != null) account.changePassword(passwords.encode(request.password()));
        accounts.flush();
        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(Role role, Long id) {
        if (role == Role.ADMIN) {
            List<UserAccount> admins = accounts.lockAccountsByRole(Role.ADMIN);
            UserAccount account = admins.stream().filter(admin -> admin.getId().equals(id)).findFirst().orElseThrow(this::notFound);
            if (admins.size() <= 1) {
                throw new ApiException(HttpStatus.CONFLICT, "FINAL_ADMIN", "At least one admin must remain.");
            }
            accounts.delete(account);
        } else {
            accounts.delete(requireAccount(role, id));
        }
        accounts.flush();
    }

    private UserAccount requireAccount(Role role, Long id) {
        return accounts.findByIdAndRole(id, role).orElseThrow(this::notFound);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Account not found in this collection.");
    }

    private ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "An account with this email already exists.");
    }
}
