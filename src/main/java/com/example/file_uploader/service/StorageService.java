package com.example.file_uploader.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadFile(MultipartFile file, String storedFileName) throws Exception;

    void deleteFile(String storedFileName) throws Exception;

    boolean fileExists(String storedFileName);

    String calculateChecksum(MultipartFile file) throws Exception;
}
