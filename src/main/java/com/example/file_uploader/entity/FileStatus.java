package com.example.file_uploader.entity;
/**
 * Enum для статуса загрузки файла.
 * 
 * PENDING - файл ожидает обработки
 * UPLOADING - файл в процессе загрузки
 * COMPLETED - файл успешно загружен
 * FAILED - произошла ошибка при загрузке
 */
public enum FileStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}
