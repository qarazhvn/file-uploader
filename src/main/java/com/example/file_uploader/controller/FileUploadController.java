package com.example.file_uploader.controller;

import com.example.file_uploader.dto.ErrorResponse;
import com.example.file_uploader.dto.FileUploadResponse;
import com.example.file_uploader.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 *      (POST /api/files/upload)
 *      (GET /api/files/{id})
 *      (GET /api/files/status/{idempotencyKey})
 *      (GET /api/files)
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload API", description = "API для асинхронной загрузки файлов")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузить файл",
            description = """
                    Загружает файл в хранилище MinIO асинхронно.
                    
                    **Особенности:**
                    - Файл принимается в формате multipart/form-data
                    - Обработка происходит асинхронно, HTTP-запрос не блокируется
                    - Для идемпотентности требуется заголовок X-Idempotency-Key
                    - При повторном запросе с тем же ключом возвращается существующий результат
                    
                    **Статус ответа 202 Accepted** означает, что файл принят в обработку.
                    Используйте GET /api/files/{id} для проверки статуса загрузки.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Файл принят в обработку",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос (пустой файл или отсутствует ключ идемпотентности)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "Файл для загрузки", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "Уникальный ключ идемпотентности для предотвращения дублирования", 
                       required = true, example = "test-upload-12345")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        
        log.info("========================================");
        log.info("HTTP POST /api/files/upload");
        log.info("Получен файл: '{}', размер: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        log.info("X-Idempotency-Key: {}", idempotencyKey);
        log.info("========================================");

        // Валидация
        if (file.isEmpty()) {
            log.warn("Попытка загрузить пустой файл");
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Отсутствует заголовок X-Idempotency-Key");
            throw new IllegalArgumentException("Заголовок X-Idempotency-Key обязателен");
        }

        FileUploadResponse response = fileUploadService.initiateUpload(file, idempotencyKey);
        
        log.info("Ответ клиенту: id={}, status={}", response.getId(), response.getStatus());
        
        // 202 Accepted - запрос принят в асинхронную обработку
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }





    @GetMapping("/{id}")
    @Operation(
            summary = "Получить информацию о файле",
            description = "Возвращает текущий статус и метаданные файла по его ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о файле",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Файл не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<FileUploadResponse> getFileById(
            @Parameter(description = "ID файла", required = true)
            @PathVariable String id) {
        
        log.info("HTTP GET /api/files/{}", id);
        
        return fileUploadService.getFileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }




    @GetMapping("/status/{idempotencyKey}")
    @Operation(
            summary = "Получить статус по ключу идемпотентности",
            description = "Возвращает статус загрузки файла по его ключу идемпотентности"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус файла",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Файл с таким ключом не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<FileUploadResponse> getFileByIdempotencyKey(
            @Parameter(description = "Ключ идемпотентности", required = true)
            @PathVariable String idempotencyKey) {
        
        log.info("HTTP GET /api/files/status/{}", idempotencyKey);
        
        return fileUploadService.getFileByIdempotencyKey(idempotencyKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    
    @GetMapping
    @Operation(
            summary = "Получить список всех файлов",
            description = "Возвращает список всех загруженных файлов, отсортированных по дате создания"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список файлов",
            content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
    )
    public ResponseEntity<List<FileUploadResponse>> getAllFiles() {
        log.info("HTTP GET /api/files");
        return ResponseEntity.ok(fileUploadService.getAllFiles());
    }
}
