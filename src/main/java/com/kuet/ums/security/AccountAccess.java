package com.kuet.ums.security;

import com.kuet.ums.entity.Role;
import com.kuet.ums.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountAccess {
    public AccountPrincipal currentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in to continue.");
        }
        return principal;
    }

    public boolean isAdmin() { return currentAccount().getRole() == Role.ADMIN; }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only admins can create or delete accounts.");
        }
    }

    public void requireCollection(Role role) {
        Role actor = currentAccount().getRole();
        if (actor != Role.ADMIN && actor != role) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Your role cannot access this resource.");
        }
    }

    public void requireOwnerOrAdmin(Role role, Long id) {
        requireCollection(role);
        if (!isAdmin() && !currentAccount().getId().equals(id)) {
            // Do not reveal whether another person's account exists.
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Account not found in this collection.");
        }
    }
}
