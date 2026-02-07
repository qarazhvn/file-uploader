package com.example.file_uploader.repository;

import com.example.file_uploader.entity.FileMetadata;
import com.example.file_uploader.entity.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с метаданными файлов.
 * Использует Spring Data JPA для автоматической генерации SQL запросов.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    Optional<FileMetadata> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<FileMetadata> findByStatus(FileStatus status);

    List<FileMetadata> findAllByOrderByCreatedAtDesc();
}
