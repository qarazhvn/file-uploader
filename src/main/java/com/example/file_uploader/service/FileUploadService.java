package com.example.file_uploader.service;

import com.example.file_uploader.dto.FileUploadResponse;
import com.example.file_uploader.entity.FileMetadata;
import com.example.file_uploader.entity.FileStatus;
import com.example.file_uploader.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для управления загрузкой файлов.
 * 
 * Реализует основную бизнес-логику:
 * - Прием файла и создание записи в БД
 * - Асинхронная загрузка в MinIO
 * - Обеспечение идемпотентности
 * - Обработка ошибок и откат изменений
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageService storageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // Инициализация загрузки файла
    @Transactional
    public FileUploadResponse initiateUpload(MultipartFile file, String idempotencyKey) {
        log.info("=== НАЧАЛО ОБРАБОТКИ ЗАПРОСА ===");
        log.info("Получен запрос на загрузку файла: '{}', idempotencyKey: '{}'", 
                file.getOriginalFilename(), idempotencyKey);

        // Проверка идемпотентности - ищем существующую запись
        Optional<FileMetadata> existingFile = fileMetadataRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existingFile.isPresent()) {
            FileMetadata metadata = existingFile.get();
            log.info("ИДЕМПОТЕНТНОСТЬ: Найдена существующая запись для idempotencyKey='{}', status={}", 
                    idempotencyKey, metadata.getStatus());
            
            return buildResponse(metadata, "Файл уже был обработан ранее (идемпотентный запрос)");
        }

        // Генерируем уникальное имя для хранения
        String storedFileName = generateStoredFileName(file.getOriginalFilename());
        
        // Вычисляем контрольную сумму
        String checksum;
        try {
            checksum = storageService.calculateChecksum(file);
            log.info("Контрольная сумма файла: {}", checksum);
        } catch (Exception e) {
            log.error("Ошибка вычисления контрольной суммы: {}", e.getMessage());
            checksum = null;
        }

        // Создаем запись в БД со статусом PENDING
        FileMetadata metadata = FileMetadata.builder()
                .idempotencyKey(idempotencyKey)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .bucketName(bucketName)
                .status(FileStatus.PENDING)
                .checksum(checksum)
                .build();

        metadata = fileMetadataRepository.save(metadata);
        log.info("Создана запись в БД с ID: {}, статус: PENDING", metadata.getId());

        // Сохраняем файл во временную директорию для асинхронной обработки
        Path tempFile = saveTempFile(file);
        
        // Запускаем асинхронную загрузку
        processUploadAsync(metadata.getId(), tempFile, file.getContentType(), file.getSize());

        log.info("Файл принят в обработку, возвращаем ответ клиенту");
        log.info("=== HTTP ЗАПРОС ЗАВЕРШЕН (не блокируем клиента) ===");
        
        return buildResponse(metadata, "Файл принят в обработку. Используйте GET /api/files/{id} для проверки статуса");
    }
















   // Реализация методов StorageService для работы с MinIO
    @Async("fileUploadExecutor")
    public CompletableFuture<Void> processUploadAsync(String fileId, Path tempFile, 
                                                       String contentType, long fileSize) {
        log.info("=== НАЧАЛО АСИНХРОННОЙ ОБРАБОТКИ (поток: {}) ===", Thread.currentThread().getName());
        log.info("Обработка файла с ID: {}", fileId);

        try {

            updateStatus(fileId, FileStatus.UPLOADING, null);
            log.info("Статус изменен на UPLOADING");

            log.info("Загрузка файла в MinIO...");
            Thread.sleep(3000); // 3 секунды задержки для демонстрации

            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Файл не найден: " + fileId));


                    
            try (var inputStream = Files.newInputStream(tempFile)) {
                storageService.uploadFile(
                        new MultipartFileWrapper(tempFile, contentType, fileSize, metadata.getOriginalFileName()),
                        metadata.getStoredFileName()
                );
            }

            updateStatusCompleted(fileId);
            log.info("Файл успешно загружен в MinIO, статус: COMPLETED");
            log.info("=== АСИНХРОННАЯ ОБРАБОТКА ЗАВЕРШЕНА УСПЕШНО ===");

        } catch (Exception e) {
            log.error("ОШИБКА при загрузке файла: {}", e.getMessage(), e);
            
            updateStatus(fileId, FileStatus.FAILED, e.getMessage());
            
            tryRollbackUpload(fileId);
            
            log.info("=== АСИНХРОННАЯ ОБРАБОТКА ЗАВЕРШЕНА С ОШИБКОЙ ===");
        } finally {
            cleanupTempFile(tempFile);
        }

        return CompletableFuture.completedFuture(null);
    }











    // Получает информацию о файле по его ID.
    public Optional<FileUploadResponse> getFileById(String id) {
        log.info("Запрос информации о файле с ID: {}", id);
        return fileMetadataRepository.findById(id)
                .map(metadata -> buildResponse(metadata, null));
    }







    // Получает информацию о файле по idempotencyKey.
    public Optional<FileUploadResponse> getFileByIdempotencyKey(String idempotencyKey) {
        log.info("Запрос информации о файле по idempotencyKey: {}", idempotencyKey);
        return fileMetadataRepository.findByIdempotencyKey(idempotencyKey)
                .map(metadata -> buildResponse(metadata, null));
    }







    // Получает список всех файлов.
    public List<FileUploadResponse> getAllFiles() {
        log.info("Запрос списка всех файлов");
        return fileMetadataRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(metadata -> buildResponse(metadata, null))
                .toList();
    }





    // Обновляет статус файла.
    @Transactional
    public void updateStatus(String fileId, FileStatus status, String errorMessage) {
        fileMetadataRepository.findById(fileId).ifPresent(metadata -> {
            metadata.setStatus(status);
            metadata.setErrorMessage(errorMessage);
            fileMetadataRepository.save(metadata);
            log.info("Статус файла {} обновлен на {}", fileId, status);
        });
    }







    // Обновляет статус файла на COMPLETED.
    @Transactional
    public void updateStatusCompleted(String fileId) {
        fileMetadataRepository.findById(fileId).ifPresent(metadata -> {
            metadata.setStatus(FileStatus.COMPLETED);
            metadata.setCompletedAt(LocalDateTime.now());
            metadata.setErrorMessage(null);
            fileMetadataRepository.save(metadata);
        });
    }





    // Генерирует уникальное имя для хранения файла в MinIO, сохраняя расширение.
    private String generateStoredFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }



    // Вычисляет контрольную сумму файла (MD5).
    private Path saveTempFile(MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory("file-uploader-");
            Path tempFile = tempDir.resolve(UUID.randomUUID().toString());
            file.transferTo(tempFile.toFile());
            log.debug("Временный файл создан: {}", tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения временного файла", e);
        }
    }






    // Удаляет временный файл после обработки.
    private void cleanupTempFile(Path tempFile) {
        try {
            if (tempFile != null && Files.exists(tempFile)) {
                Path tempDir = tempFile.getParent();
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempDir);
                log.info("Временные ресурсы очищены: {}", tempFile);
            }
        } catch (IOException e) {
            log.warn("Ошибка удаления временного файла: {}", e.getMessage());
        }
    }





    // Пытается удалить файл из MinIO при ошибке загрузки для отката изменений.
    private void tryRollbackUpload(String fileId) {
        try {
            fileMetadataRepository.findById(fileId).ifPresent(metadata -> {
                if (storageService.fileExists(metadata.getStoredFileName())) {
                    try {
                        storageService.deleteFile(metadata.getStoredFileName());
                        log.info("Откат: файл удален из MinIO");
                    } catch (Exception e) {
                        log.error("Ошибка отката загрузки: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Ошибка при попытке отката: {}", e.getMessage());
        }
    }

    


    // Строит DTO для ответа на основе метаданных файла.
    private FileUploadResponse buildResponse(FileMetadata metadata, String message) {
        return FileUploadResponse.builder()
                .id(metadata.getId())
                .idempotencyKey(metadata.getIdempotencyKey())
                .originalFileName(metadata.getOriginalFileName())
                .fileSize(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .status(metadata.getStatus())
                .errorMessage(metadata.getErrorMessage())
                .checksum(metadata.getChecksum())
                .createdAt(metadata.getCreatedAt())
                .completedAt(metadata.getCompletedAt())
                .message(message)
                .build();
    }



    // Временная обертка для MultipartFile, чтобы использовать его с MinIO SDK.
    private static class MultipartFileWrapper implements MultipartFile {
        private final Path path;
        private final String contentType;
        private final long size;
        private final String originalFilename;

        public MultipartFileWrapper(Path path, String contentType, long size, String originalFilename) {
            this.path = path;
            this.contentType = contentType;
            this.size = size;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() { return "file"; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return size == 0; }

        @Override
        public long getSize() { return size; }

        @Override
        public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }

        @Override
        public java.io.InputStream getInputStream() throws IOException { 
            return Files.newInputStream(path); 
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.copy(path, dest.toPath());
        }
    }
}
