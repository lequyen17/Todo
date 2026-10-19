package com.example.todo_api.modules.tasks.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.example.todo_api.modules.tasks.entity.Priority;

class TaskRequestValidationTest {

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
    @ValueSource(strings = { "", "   ", "\t\n" })
    void title_blank_isRejected(String title) {
        assertThat(invalidFields(create(title))).containsExactly("title");
    }

    @Test
    void title_isTrimmedAndBoundedTo200() {
        CreateTaskRequest req = create("  Buy milk  ");

        assertThat(req.title()).isEqualTo("Buy milk");
        assertThat(invalidFields(req)).isEmpty();
        assertThat(invalidFields(create("x".repeat(200)))).isEmpty();
        assertThat(invalidFields(create("x".repeat(201)))).containsExactly("title");
    }

    @Test
    void description_boundedTo2000_andBlankBecomesNull() {
        assertThat(invalidFields(new CreateTaskRequest("T", "x".repeat(2000), null, null, false, null))).isEmpty();
        assertThat(invalidFields(new CreateTaskRequest("T", "x".repeat(2001), null, null, false, null)))
                .containsExactly("description");
        assertThat(new CreateTaskRequest("T", "   ", null, null, false, null).description()).isNull();
    }

    @Test
    void update_requiresPriority() {
        assertThat(invalidFields(new UpdateTaskRequest("T", null, null, null, false, null)))
                .containsExactly("priority");
        assertThat(invalidFields(new UpdateTaskRequest("T", null, Priority.LOW, null, false, null))).isEmpty();
    }

    private static CreateTaskRequest create(String title) {
        return new CreateTaskRequest(title, null, null, null, false, null);
    }

    private static <T> Set<String> invalidFields(T request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
