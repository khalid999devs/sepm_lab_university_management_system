package com.kuet.ums.dto;

import com.kuet.ums.entity.Notice;
import java.time.Instant;

public record NoticeResponse(
    Long id,
    String title,
    String body,
    Instant publishedAt,
    String authorName
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
            notice.getId(),
            notice.getTitle(),
            notice.getBody(),
            notice.getPublishedAt(),
            notice.getAuthor() == null ? "University office" : notice.getAuthor().getFullName()
        );
    }
}
