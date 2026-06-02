package com.codeit.monew.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String NotificationAsyncName = "notificationTaskExecutor";

    @Bean(name = NotificationAsyncName)
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);                 // 기본적으로 대기하고 있는 스레드 수
        executor.setMaxPoolSize(20);                 // 최대 스레드 수
        executor.setQueueCapacity(100);              // 큐에 대기할 수 있는 작업의 수
        executor.setThreadNamePrefix("Async-Notification-"); // 스레드 이름 접두사 (로깅 시 사용)
        executor.initialize();
        return executor;
    }
}

