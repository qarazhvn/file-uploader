package com.example.file_uploader.dto;

import com.example.file_uploader.entity.FileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO для ответа при загрузке файла.
@Data
@Builder    
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о загруженном файле")
public class FileUploadResponse {

    @Schema(description = "Уникальный идентификатор файла", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Ключ идемпо  тентности", example = "test")
    private String idempotencyKey;

    @Schema(description = "Оригинальное имя файла", example = "DOCUMENT.pdf")
    private String originalFileName;

    @Schema(description = "Размер файла в байтах", example = "1234567")
    private Long fileSize;

    @Schema(description = "MIME тип файла", example = "application/pdf")
    private String contentType;

    @Schema(description = "Статус загрузки", example = "FAILED")
    private FileStatus status;

    @Schema(description = "Сообщение об ошибке (если есть)", example = "null")
    private String errorMessage;

    @Schema(description = "Контрольная сумма файла", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String checksum;

    @Schema(description = "Дата и время создания")
    private LocalDateTime createdAt;

    @Schema(description = "Дата и время завершения загрузки")
    private LocalDateTime completedAt;

    @Schema(description = "Сообщение о результате операции", example = "Файл успешно принят в обработку")
    private String message;
}
