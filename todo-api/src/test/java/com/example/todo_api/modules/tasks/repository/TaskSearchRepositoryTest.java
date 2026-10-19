package com.example.todo_api.modules.tasks.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskSearchRepositoryTest {

    @Test
    void escapeLike_treatsWildcardsAsPlainText() {
        assertThat(TaskSearchRepository.escapeLike("50%_done\\")).isEqualTo("50\\%\\_done\\\\");
        assertThat(TaskSearchRepository.escapeLike("assign")).isEqualTo("assign");
    }
}
