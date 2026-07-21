package com.taskflow.taskservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles file storage for task attachments.
 *
 * Files are saved to a local directory (configurable via app.file-upload-dir).
 * Each file is stored with a UUID prefix to avoid name collisions.
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public FileStorageService(@Value("${app.file-upload-dir:./task-files}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            logger.info("[FileStorage] Upload directory: {}", this.uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create upload directory: " + uploadDirPath, ex);
        }
    }

    /**
     * Saves a file to the upload directory.
     * Returns the generated unique filename (used to retrieve the file later).
     *
     * @param file the uploaded file
     * @return stored filename (UUID + original extension)
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Generate unique filename to avoid collisions
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Path targetPath = this.uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("[FileStorage] Stored file: {} as {}", originalName, storedName);
            return storedName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file: " + originalName, ex);
        }
    }

    /**
     * Loads a file as a downloadable Resource.
     *
     * @param storedFileName the UUID filename returned by storeFile()
     * @return the file as a Resource for streaming to the client
     */
    public Resource loadFile(String storedFileName) {
        try {
            Path filePath = this.uploadDir.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new IllegalArgumentException("File not found: " + storedFileName);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid file path: " + storedFileName, ex);
        }
    }

    /**
     * Deletes a stored file (used when replacing an existing upload).
     *
     * @param storedFileName the UUID filename to delete
     */
    public void deleteFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) return;
        try {
            Path filePath = this.uploadDir.resolve(storedFileName).normalize();
            Files.deleteIfExists(filePath);
            logger.info("[FileStorage] Deleted file: {}", storedFileName);
        } catch (IOException ex) {
            logger.warn("[FileStorage] Could not delete file {}: {}", storedFileName, ex.getMessage());
        }
    }
}
