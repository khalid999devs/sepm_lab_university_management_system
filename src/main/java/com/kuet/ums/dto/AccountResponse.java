package com.kuet.ums.dto;

import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;

public record AccountResponse(Long id, String fullName, String email, Role role) {
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(), account.getRole());
    }
}
