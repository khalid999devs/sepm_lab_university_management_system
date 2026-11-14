package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuet.ums.dto.CreateAccountRequest;
import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.security.AccountAccess;
import com.kuet.ums.service.AccountService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceUnitTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AccountAccess access = mock(AccountAccess.class);
    private final AccountService service = new AccountService(accounts, passwords, access);

    @Test
    void creatingAStudentHashesThePasswordAndFixesTheRole() {
        var request = new CreateAccountRequest(
            "  Amina Rahman  ",
            "  AMINA@EXAMPLE.COM  ",
            "AminaPass123"
        );
        when(passwords.encode("AminaPass123")).thenReturn("encoded-password");
        when(accounts.saveAndFlush(any(UserAccount.class)))
            .thenAnswer(call -> call.getArgument(0));

        var response = service.create(Role.STUDENT, request);

        var savedAccount = ArgumentCaptor.forClass(UserAccount.class);
        verify(access).requireAdmin();
        verify(accounts).saveAndFlush(savedAccount.capture());
        assertThat(savedAccount.getValue().getEmail()).isEqualTo("amina@example.com");
        assertThat(savedAccount.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedAccount.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.fullName()).isEqualTo("Amina Rahman");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void deletingTheLastAdminFailsBeforeDeletingTheRecord() {
        UserAccount lastAdmin = mock(UserAccount.class);
        when(lastAdmin.getId()).thenReturn(7L);
        when(accounts.lockAccountsByRole(Role.ADMIN)).thenReturn(List.of(lastAdmin));

        assertThatThrownBy(() -> service.delete(Role.ADMIN, 7L))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(error.getCode()).isEqualTo("FINAL_ADMIN");
            });

        verify(accounts, never()).delete(any(UserAccount.class));
    }
}
