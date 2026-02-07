package com.example.file_uploader.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/// Сущность для хранения метаданных файлов в базе данных.
@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Ключ идемпотентности.
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    // Оригинальное имя файла.
    @Column(nullable = false)
    private String originalFileName;

    // Имя файла в хранилище MinIO.
    @Column(nullable = false)
    private String storedFileName;

    // MIME-тип файла.
    private String contentType;

    // Размер файла в байтах.
    private Long fileSize;

    // Название бакета в MinIO.
    @Column(nullable = false)
    private String bucketName;

    // Статус загрузки.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status;

    // Сообщение об ошибке, если загрузка не удалас.
    @Column(length = 1000)
    private String errorMessage;

    // Контрольная сумма файла.
    private String checksum;

    // Дата и время создания записи.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Дата и время последнего обновления.
    private LocalDateTime updatedAt;

    // Дата и время завершения загрузки.
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
