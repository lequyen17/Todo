package com.example.todo_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /**
     * Thread pool riêng để gửi mail ngoài luồng request: SMTP chậm/lỗi không làm chậm hay làm hỏng API.
     * Hàng đợi đầy thì bỏ mail (ghi log) thay vì chặn request; người dùng vẫn bấm "gửi lại" được.
     */
    @Bean(destroyMethod = "shutdown")
    ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler((task, pool) -> log.error("Mail queue is full, email dropped"));
        // Tắt app: gửi nốt mail đang chờ (tối đa 10s)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
