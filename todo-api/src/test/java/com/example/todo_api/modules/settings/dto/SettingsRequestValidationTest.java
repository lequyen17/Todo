package com.example.todo_api.modules.settings.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class SettingsRequestValidationTest {

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

    /** VR-12: 1 phút .. 7 ngày (10080 phút). */
    @ParameterizedTest
    @ValueSource(ints = { 1, 15, 60, 120, 1440, 10080 })
    void remindBefore_withinOneMinuteToSevenDays_isValid(int minutes) {
        assertThat(invalidFields(request(minutes))).isEmpty();
    }

    /** FR-05.2 AC6 (0, số âm), AC7 (8 ngày). */
    @ParameterizedTest
    @ValueSource(ints = { 0, -5, 10081, 8 * 1440 })
    void remindBefore_outOfRange_isRejected(int minutes) {
        assertThat(invalidFields(request(minutes))).containsExactly("remindBeforeMinutes");
    }

    private static UpdateSettingsRequest request(int minutes) {
        return new UpdateSettingsRequest(true, true, true, minutes);
    }

    private static Set<String> invalidFields(UpdateSettingsRequest request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
