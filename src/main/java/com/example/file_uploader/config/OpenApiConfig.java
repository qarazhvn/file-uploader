package com.example.file_uploader.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI/Swagger
 * Swagger UI: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("File Uploader API")
                        .version("1.0.0")
                        .description("""
                                REST API для асинхронной загрузки файлов с использованием MinIO.
                                
                                **Основные возможности:**
                                - Загрузка файлов в формате multipart/form-data
                                - Асинхронная обработка загрузки
                                - Идемпотентность через уникальный idempotency-key
                                - Отслеживание статуса загрузки
                                - Получение информации о загруженных файлах
                                
                                **Архитектурные решения:**
                                - Использование MinIO как S3-совместимого хранилища
                                - PostgreSQL для хранения метаданных файлов
                                - Асинхронная обработка через @Async
                                """)
                        .contact(new Contact()
                                .name("Developer")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
