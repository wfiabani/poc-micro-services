package com.example.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class NotificationExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService notificationExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
