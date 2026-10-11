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
@RequestMapping("/api/teachers")
public class TeacherController {
    private final AccountService accounts;

    public TeacherController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accounts.list(Role.TEACHER);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accounts.get(Role.TEACHER, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accounts.create(Role.TEACHER, request);
        return ResponseEntity.created(URI.create("/api/teachers/" + account.id())).body(account);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request) {
        return accounts.update(Role.TEACHER, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accounts.delete(Role.TEACHER, id);
        return ResponseEntity.noContent().build();
    }
}
