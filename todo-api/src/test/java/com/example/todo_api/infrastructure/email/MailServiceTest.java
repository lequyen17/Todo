package com.example.todo_api.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.todo_api.config.AppProperties;

class MailServiceTest {

    private JavaMailSender mailSender;
    private List<Runnable> queued;
    private MailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        queued = new ArrayList<>();
        Executor recordingExecutor = queued::add;
        AppProperties props = new AppProperties("http://localhost",
                new AppProperties.Mail("noreply@example.com", true), null,
                new AppProperties.Otp(Duration.ofMinutes(5), 5, Duration.ofSeconds(60)),
                new AppProperties.PasswordReset(Duration.ofMinutes(15)), null,
                new AppProperties.Google(null), new AppProperties.R2(null, null, null, null, null));
        service = new MailService(mailSender, props, recordingExecutor);
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void send_doesNotCallSmtpOnCallerThread() {
        service.sendLoginOtp("a@example.com", "A", "123456");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(queued).hasSize(1);

        runQueued();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailure_isLoggedNotThrown() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> {
            service.sendPasswordReset("a@example.com", "A", "http://link");
            runQueued();
        }).doesNotThrowAnyException();
    }

    @Test
    void insideTransaction_sendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.sendRegistrationOtp("a@example.com", "A", "123456");
        assertThat(queued).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(queued).hasSize(1);
    }

    @Test
    void insideTransaction_rollbackSendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        service.sendRegistrationOtp("a@example.com", "A", "123456");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(queued).isEmpty();
    }

    private void runQueued() {
        queued.forEach(Runnable::run);
        queued.clear();
    }
}
