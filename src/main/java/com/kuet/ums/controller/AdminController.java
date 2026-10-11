package com.kuet.ums.controller;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.Role;
import com.kuet.ums.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AccountService accounts;

    public AdminController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accounts.list(Role.ADMIN);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accounts.get(Role.ADMIN, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accounts.create(Role.ADMIN, request);
        return ResponseEntity.created(URI.create("/api/admins/" + account.id())).body(account);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request) {
        return accounts.update(Role.ADMIN, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accounts.delete(Role.ADMIN, id);
        return ResponseEntity.noContent().build();
    }
}
