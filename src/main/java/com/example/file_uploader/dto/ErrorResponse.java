package com.example.file_uploader.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//  DTO для ответа с информацией об ошибке.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация об ошибке")
public class ErrorResponse {

    @Schema(description = "HTTP статус код", example = "400")
    private int status;

    @Schema(description = "Тип ошибки", example = "BAD_REQUEST")
    private String error;

    @Schema(description = "Сообщение об ошибке", example = "Файл не может быть пустым")
    private String message;

    @Schema(description = "Путь запроса", example = "/api/files/upload")
    private String path;

    @Schema(description = "Время возникновения ошибки")
    private LocalDateTime timestamp;
}
