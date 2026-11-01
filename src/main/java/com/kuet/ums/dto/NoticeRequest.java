package com.kuet.ums.dto;

import jakarta.validation.constraints.*;

public record NoticeRequest(@NotBlank @Size(min = 2, max = 150) String title,
                            @NotBlank @Size(min = 2, max = 3000) String body) {
    public NoticeRequest {
        title = title == null ? null : title.strip();
        body = body == null ? null : body.strip();
    }
}
