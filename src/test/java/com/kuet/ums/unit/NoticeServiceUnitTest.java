package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuet.ums.dto.NoticeRequest;
import com.kuet.ums.entity.Notice;
import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.repository.NoticeRepository;
import com.kuet.ums.security.AccountAccess;
import com.kuet.ums.security.AccountPrincipal;
import com.kuet.ums.service.NoticeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// Tests the notice rules with fake (mocked) repositories, so no database is needed.
class NoticeServiceUnitTest {

    private final NoticeRepository notices = mock(NoticeRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountAccess access = mock(AccountAccess.class);
    private final NoticeService service = new NoticeService(notices, accounts, access);

    @Test
    void adminCreatesANoticeWithTheirNameAsAuthor() {
        // The signed-in admin has id 1 and is called "Admin User".
        AccountPrincipal signedIn = mock(AccountPrincipal.class);
        when(signedIn.getId()).thenReturn(1L);
        when(access.currentAccount()).thenReturn(signedIn);
        var admin = new UserAccount("Admin User", "admin@test.edu", "hash", Role.ADMIN);
        when(accounts.findById(1L)).thenReturn(Optional.of(admin));
        when(notices.saveAndFlush(any(Notice.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(new NoticeRequest("  Exam routine  ", "Starts on Monday."));

        verify(access).requireRole(Role.ADMIN);
        assertThat(response.title()).isEqualTo("Exam routine");
        assertThat(response.authorName()).isEqualTo("Admin User");
    }

    @Test
    void openingAMissingNoticeGivesNotFound() {
        when(notices.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(error.getCode()).isEqualTo("NOT_FOUND");
            });
    }
}
