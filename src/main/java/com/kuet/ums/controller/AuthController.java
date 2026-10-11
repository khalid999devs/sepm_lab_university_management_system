package com.kuet.ums.controller;

import com.kuet.ums.dto.AccountResponse;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.security.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository accounts;

    public AuthController(AccountRepository accounts) { this.accounts = accounts; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return accounts.findById(principal.getId()).map(AccountResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in again."));
    }
}
