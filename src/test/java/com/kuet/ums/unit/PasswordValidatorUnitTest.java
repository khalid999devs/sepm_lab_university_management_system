package com.kuet.ums.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuet.ums.validation.PasswordValidator;
import org.junit.jupiter.api.Test;

class PasswordValidatorUnitTest {

    @Test
    void acceptsAValidPassword() {
        assertThat(PasswordValidator.isValidPassword("Pass1234")).isTrue();
    }

    @Test
    void rejectsPasswordsShorterThanEightCharacters() {
        assertThat(PasswordValidator.isValidPassword("Pass123")).isFalse();
    }

    @Test
    void checksTheBcryptLimitInBytes() {
        // Each é takes two UTF-8 bytes: 36 fit in 72 bytes, but 37 do not.
        assertThat(PasswordValidator.isValidPassword("é".repeat(36))).isTrue();
        assertThat(PasswordValidator.isValidPassword("é".repeat(37))).isFalse();
    }
}
