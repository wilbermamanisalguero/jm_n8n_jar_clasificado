package com.pe.jm_n8n_jar_clasificado.service;

import com.pe.jm_n8n_jar_clasificado.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilePostProcessService {

    private final AppProperties properties;

    public void processSuccessfulUpload(File file) {
        switch (properties.getSuccessAction()) {
            case DELETE -> deleteFile(file);
            case MOVE -> moveToProcessed(file);
        }
    }

    public void processFailedUpload(File file) {
        if (properties.getErrorFolder() != null && !properties.getErrorFolder().isBlank()) {
            moveToError(file);
        } else {
            log.warn("File {} upload failed but no error folder configured. File remains in input folder.",
                    file.getName());
        }
    }

    private void deleteFile(File file) {
        try {
            if (file.delete()) {
                log.info("Deleted file successfully: {}", file.getName());
            } else {
                log.error("Failed to delete file: {}", file.getName());
            }
        } catch (Exception e) {
            log.error("Error deleting file {}: {}", file.getName(), e.getMessage(), e);
        }
    }

    private void moveToProcessed(File file) {
        String processedFolder = properties.getProcessedFolder();
        if (processedFolder == null || processedFolder.isBlank()) {
            log.error("Processed folder not configured but MOVE action is set. Cannot move file: {}",
                    file.getName());
            return;
        }

        moveFile(file, processedFolder, "processed");
    }

    private void moveToError(File file) {
        String errorFolder = properties.getErrorFolder();
        moveFile(file, errorFolder, "error");
    }

    private void moveFile(File file, String targetFolder, String folderType) {
        try {
            Path targetDir = Path.of(targetFolder);

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                log.info("Created {} folder: {}", folderType, targetFolder);
            }

            Path targetPath = targetDir.resolve(file.getName());

            if (Files.exists(targetPath)) {
                String newName = generateUniqueFileName(file.getName());
                targetPath = targetDir.resolve(newName);
                log.warn("Target file already exists, using new name: {}", newName);
            }

            Files.move(file.toPath(), targetPath, StandardCopyOption.ATOMIC_MOVE);
            log.info("Moved file {} to {} folder successfully", file.getName(), folderType);

        } catch (IOException e) {
            log.error("Failed to move file {} to {} folder: {}", file.getName(), folderType, e.getMessage(), e);
        }
    }

    private String generateUniqueFileName(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
        String extension = dotIndex > 0 ? originalName.substring(dotIndex) : "";
        long timestamp = System.currentTimeMillis();
        return nameWithoutExt + "_" + timestamp + extension;
    }
}
