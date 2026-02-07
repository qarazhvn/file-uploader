package com.example.file_uploader.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Конфигурация для MinIO клиента.
@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // Создание MinIO клиента как Spring Bean
    @Bean
    public MinioClient minioClient() {
        log.info("Создание MinIO клиента с endpoint: {}", endpoint);
        
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    // Инициализация bucket при старте приложения
    @PostConstruct
    public void initBucket() {
        try {
            MinioClient client = minioClient();
            boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            
            if (!bucketExists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Bucket '{}' успешно создан", bucketName);
            } else {
                log.info("Bucket '{}' уже существует", bucketName);
            }
        } catch (Exception e) {
            log.warn("Не удалось инициализировать bucket при старте: {}. " +
                    "Bucket будет создан при первой загрузке файла.", e.getMessage());
        }
    }
}
