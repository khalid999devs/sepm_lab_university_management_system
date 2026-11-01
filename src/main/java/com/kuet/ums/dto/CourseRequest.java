package com.kuet.ums.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record CourseRequest(
    @NotBlank
    @Pattern(
        regexp = "[A-Z][A-Z0-9 -]{1,19}",
        message = "Use 2–20 letters, numbers, spaces or hyphens, starting with a letter."
    )
    String code,
    @NotBlank @Size(min = 2, max = 150) String title,
    @NotNull @Min(1) @Max(6) Integer credits,
    @NotNull @Min(1) @Max(200) Integer capacity,
    @NotNull @Positive Long teacherId
) {
    public CourseRequest {
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        title = title == null ? null : title.strip();
    }
}
