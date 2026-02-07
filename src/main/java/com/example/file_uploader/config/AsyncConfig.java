package com.example.file_uploader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Конфигурация асинхронной обработки.
 * 
 * Использует ThreadPoolTaskExecutor для управления пулом потоков,
 * что обеспечивает эффективную обработку асинхронных задач загрузки файлов.
 * 
 * Применение принципа Single Responsibility (SOLID):
 * Этот класс отвечает только за конфигурацию асинхронного выполнения.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Value("${async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Создает executor для асинхронных задач загрузки файлов.
     * 
     * @return настроенный ThreadPoolTaskExecutor
     */
    @Bean(name = "fileUploadExecutor")
    public Executor fileUploadExecutor() {
        log.info("Инициализация ThreadPoolTaskExecutor для загрузки файлов");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("FileUpload-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("ThreadPoolTaskExecutor инициализирован: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
}
