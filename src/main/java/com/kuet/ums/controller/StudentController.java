package com.kuet.ums.controller;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.Role;
import com.kuet.ums.service.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final AccountService accounts;

    public StudentController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accounts.list(Role.STUDENT);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accounts.get(Role.STUDENT, id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> create(
        @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse account = accounts.create(Role.STUDENT, request);

        return ResponseEntity.created(URI.create("/api/students/" + account.id())).body(account);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAccountRequest request
    ) {
        return accounts.update(Role.STUDENT, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accounts.delete(Role.STUDENT, id);

        return ResponseEntity.noContent().build();
    }
}
