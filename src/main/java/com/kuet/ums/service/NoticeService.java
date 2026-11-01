package com.kuet.ums.service;

import com.kuet.ums.dto.*;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.*;
import com.kuet.ums.security.AccountAccess;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository notices;
    private final AccountRepository accounts;
    private final AccountAccess access;

    public NoticeService(
        NoticeRepository notices,
        AccountRepository accounts,
        AccountAccess access
    ) {
        this.notices = notices;
        this.accounts = accounts;
        this.access = access;
    }

    public List<NoticeResponse> list() {
        access.currentAccount();

        return notices
            .findAllByOrderByPublishedAtDescIdDesc()
            .stream()
            .map(NoticeResponse::from)
            .toList();
    }

    public NoticeResponse get(Long id) {
        access.currentAccount();

        return NoticeResponse.from(requireNotice(id));
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        access.requireRole(Role.ADMIN);

        UserAccount author = accounts
            .findById(access.currentAccount().getId())
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED",
                    "Please sign in again."
                )
            );

        return NoticeResponse.from(
            notices.saveAndFlush(new Notice(request.title(), request.body(), author))
        );
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        access.requireRole(Role.ADMIN);

        Notice notice = requireNotice(id);
        notice.update(request.title(), request.body());
        notices.flush();

        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        access.requireRole(Role.ADMIN);

        notices.delete(requireNotice(id));
        notices.flush();
    }

    private Notice requireNotice(Long id) {
        return notices
            .findById(id)
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notice not found.")
            );
    }
}
