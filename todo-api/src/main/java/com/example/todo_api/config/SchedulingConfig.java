package com.example.todo_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Job định kỳ (ReminderJob) chạy trên scheduler riêng: WebSocket cũng đăng ký TaskScheduler của nó,
 * nên phải chỉ định rõ, tránh @Scheduled chạy chung luồng với nhịp tim WebSocket.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(jobScheduler());
    }

    @Bean
    ThreadPoolTaskScheduler jobScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("job-");
        return scheduler;
    }

    /**
     * Gửi email nhắc nhở ngoài luồng job: SMTP chậm hoặc đang thử lại không làm trễ các reminder khác.
     * Hàng đợi đầy thì bỏ email (ghi log); thông báo trong web không bị ảnh hưởng.
     */
    @Bean(destroyMethod = "shutdown")
    ThreadPoolTaskExecutor reminderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("reminder-mail-");
        executor.setRejectedExecutionHandler((task, pool) -> log.error("Reminder email queue is full, email dropped"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
