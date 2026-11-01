package com.kuet.ums.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuet.ums.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.Locale;

public record CreateAccountRequest(
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotNull @ValidPassword @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password
) {
    public CreateAccountRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CreateAccountRequest[credentials omitted]";
    }
}
