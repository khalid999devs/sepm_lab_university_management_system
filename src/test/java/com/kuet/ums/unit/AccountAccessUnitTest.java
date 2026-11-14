package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kuet.ums.entity.Role;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.security.AccountAccess;
import com.kuet.ums.security.AccountPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AccountAccessUnitTest {

    private final AccountAccess access = new AccountAccess();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanOpenAnotherPersonsAccount() {
        signInAs(Role.ADMIN, 1L);

        assertThatCode(() -> access.requireOwnerOrAdmin(Role.STUDENT, 20L))
            .doesNotThrowAnyException();
    }

    @Test
    void teacherCanOpenOwnAccountButNotAnotherTeachersAccount() {
        signInAs(Role.TEACHER, 2L);

        assertThatCode(() -> access.requireOwnerOrAdmin(Role.TEACHER, 2L))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireOwnerOrAdmin(Role.TEACHER, 3L))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(error.getCode()).isEqualTo("NOT_FOUND");
            });
    }

    @Test
    void studentCannotPerformAdminOnlyActions() {
        signInAs(Role.STUDENT, 4L);

        assertThatThrownBy(access::requireAdmin)
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(error.getCode()).isEqualTo("FORBIDDEN");
            });
    }

    @Test
    void missingLoginIsRejected() {
        assertThatThrownBy(access::currentAccount)
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(error.getCode()).isEqualTo("UNAUTHENTICATED");
            });
    }

    private void signInAs(Role role, Long id) {
        AccountPrincipal principal = mock(AccountPrincipal.class);
        when(principal.getRole()).thenReturn(role);
        when(principal.getId()).thenReturn(id);

        var authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
