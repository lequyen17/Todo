package com.example.todo_api.modules.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AuthRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc12345", "Password1", "mậtkhẩu123", "a1b2c3d4!@#" })
    void password_withLettersAndDigits_isValid(String password) {
        assertThat(invalidFields(register("a@example.com", password, "Alice"))).doesNotContain("password");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abcdefgh", "12345678", "!!!!!!!!", "abc1234", "" })
    void password_missingLetterOrDigitOrTooShort_isRejected(String password) {
        assertThat(invalidFields(register("a@example.com", password, "Alice"))).contains("password");
    }

    @Test
    void password_ruleAppliesToChangeAndReset() {
        assertThat(invalidFields(new ChangePasswordRequest("old", "onlyletters"))).containsExactly("newPassword");
        assertThat(invalidFields(new ResetPasswordRequest("token", "12345678"))).containsExactly("newPassword");
        assertThat(invalidFields(new ChangePasswordRequest("old", "letters123"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "A", "  a  ", "", "   " })
    void displayName_shorterThanTwoCharsAfterTrim_isRejected(String displayName) {
        assertThat(invalidFields(register("a@example.com", "abc12345", displayName))).contains("displayName");
    }

    @Test
    void displayName_isTrimmedAndBoundedTo50() {
        RegisterRequest req = register("a@example.com", "abc12345", "  An  ");

        assertThat(req.displayName()).isEqualTo("An");
        assertThat(invalidFields(req)).isEmpty();
        assertThat(invalidFields(register("a@example.com", "abc12345", "x".repeat(50)))).isEmpty();
        assertThat(invalidFields(register("a@example.com", "abc12345", "x".repeat(51)))).contains("displayName");
    }

    @Test
    void passwordErrorMessage_explainsTheRule() {
        Set<String> messages = validator.validate(register("a@example.com", "abcdefgh", "Alice")).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).containsExactly("Password must contain both letters and numbers");
    }

    private static RegisterRequest register(String email, String password, String displayName) {
        return new RegisterRequest(email, password, displayName);
    }

    private static <T> Set<String> invalidFields(T request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
