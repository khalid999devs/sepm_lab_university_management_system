package com.kuet.ums.service;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.security.AccountAccess;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final AccountAccess access;

    public AccountService(
        AccountRepository accounts,
        PasswordEncoder passwords,
        AccountAccess access
    ) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.access = access;
    }

    public List<AccountResponse> list(Role role) {
        access.requireCollection(role);

        if (!access.isAdmin()) {
            return List.of(
                AccountResponse.from(requireAccount(role, access.currentAccount().getId()))
            );
        }

        return accounts
            .findAllByRoleOrderByIdAsc(role)
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    public AccountResponse get(Role role, Long id) {
        access.requireOwnerOrAdmin(role, id);

        return AccountResponse.from(requireAccount(role, id));
    }

    @Transactional
    public AccountResponse create(Role role, CreateAccountRequest request) {
        access.requireAdmin();

        if (accounts.existsByEmail(request.email())) {
            throw duplicateEmail();
        }

        UserAccount account = new UserAccount(
            request.fullName(),
            request.email(),
            passwords.encode(request.password()),
            role
        );

        return AccountResponse.from(accounts.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse update(Role role, Long id, UpdateAccountRequest request) {
        access.requireOwnerOrAdmin(role, id);

        UserAccount account = requireAccount(role, id);
        if (accounts.existsByEmailAndIdNot(request.email(), id)) {
            throw duplicateEmail();
        }

        account.updateProfile(request.fullName(), request.email());
        if (request.password() != null) {
            account.changePassword(passwords.encode(request.password()));
        }

        accounts.flush();

        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(Role role, Long id) {
        access.requireAdmin();

        if (role == Role.ADMIN) {
            List<UserAccount> admins = accounts.lockAccountsByRole(Role.ADMIN);
            UserAccount account = admins
                .stream()
                .filter((admin) -> admin.getId().equals(id))
                .findFirst()
                .orElseThrow(this::notFound);
            if (admins.size() <= 1) {
                throw new ApiException(
                    HttpStatus.CONFLICT,
                    "FINAL_ADMIN",
                    "At least one admin must remain."
                );
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
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "Account not found in this collection."
        );
    }

    private ApiException duplicateEmail() {
        return new ApiException(
            HttpStatus.CONFLICT,
            "DUPLICATE_EMAIL",
            "An account with this email already exists."
        );
    }
}
