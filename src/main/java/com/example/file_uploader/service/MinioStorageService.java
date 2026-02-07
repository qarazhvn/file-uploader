package com.example.file_uploader.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

// Сервис для работы с MinIO - загрузка, удаление и проверка файлов.
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file, String storedFileName) throws Exception {
        log.info("Начало загрузки файла '{}' в MinIO bucket '{}'", storedFileName, bucketName);
        
        // Проверяем и создаем bucket если не существует
        ensureBucketExists();

        try (InputStream inputStream = file.getInputStream()) {
            // Загружаем файл в MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storedFileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            log.info("Файл '{}' успешно загружен в MinIO", storedFileName);
            return String.format("%s/%s", bucketName, storedFileName);
        }
    }



    
    @Override
    public void deleteFile(String storedFileName) throws Exception {
        log.info("Удаление файла '{}' из MinIO bucket '{}'", storedFileName, bucketName);
        
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storedFileName)
                        .build()
        );
        
        log.info("Файл '{}' успешно удален из MinIO", storedFileName);
    }




    @Override
    public boolean fileExists(String storedFileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storedFileName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("Ошибка при проверке существования файла: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования файла: {}", e.getMessage());
            return false;
        }
    }




    @Override
    public String calculateChecksum(MultipartFile file) throws Exception {
        log.debug("Вычисление контрольной суммы для файла '{}'", file.getOriginalFilename());
        
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        String checksum = HexFormat.of().formatHex(md.digest());
        log.debug("Контрольная сумма: {}", checksum);
        return checksum;
    }




    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        
        if (!exists) {
            log.info("Bucket '{}' не существует, создаем...", bucketName);
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Bucket '{}' успешно создан", bucketName);
        }
    }
}
