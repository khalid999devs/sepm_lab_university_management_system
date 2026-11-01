package com.kuet.ums.dto;

import jakarta.validation.constraints.*;

public record EnrollmentRequest(@NotNull @Positive Long courseId, @Positive Long studentId) {}
