package com.pe.jm_n8n_jar_clasificado.service;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pe.jm_n8n_jar_clasificado.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilePostProcessService {

    private final AppProperties properties;

    public void processSuccessfulUpload(File file, String jsonResponse) {
        String targetFolder = properties.getProcessedFolder();

        switch (properties.getSuccessAction()) {
            case DELETE:
                saveJsonFile(file, jsonResponse, properties.getInputFolder());
                deleteFile(file);
                break;
            case MOVE:
                moveToProcessed(file);
                saveJsonFile(file, jsonResponse, targetFolder);
                break;
        }
    }

    public void processFailedUpload(File file, String jsonResponse) {
        if (properties.getErrorFolder() != null && !properties.getErrorFolder().isBlank()) {
            moveToError(file);
            saveJsonFile(file, jsonResponse, properties.getErrorFolder());
        } else {
            log.warn("File {} upload failed but no error folder configured. File remains in input folder.",
                    file.getName());
            saveJsonFile(file, jsonResponse, properties.getInputFolder());
        }
    }

    private void saveJsonFile(File originalFile, String jsonContent, String targetFolder) {
        if (jsonContent == null || jsonContent.isBlank()) {
            log.warn("No JSON content to save for file: {}", originalFile.getName());
            return;
        }

        try {
            String jsonFileName = getJsonFileName(originalFile.getName());
            Path targetDir = Path.of(targetFolder);

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path jsonFilePath = targetDir.resolve(jsonFileName);

            // Formatear JSON con indentación (beautify)
            String formattedJson = formatJsonPretty(jsonContent);
            Files.writeString(jsonFilePath, formattedJson, StandardCharsets.UTF_8);
            log.info("Saved JSON response to: {}", jsonFilePath);

        } catch (IOException e) {
            log.error("Failed to save JSON file for {}: {}", originalFile.getName(), e.getMessage(), e);
        }
    }

    private String formatJsonPretty(String jsonContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Configurar PrettyPrinter para que los arrays tengan saltos de línea
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

            Object json = mapper.readValue(jsonContent, Object.class);
            return mapper.writer(prettyPrinter).writeValueAsString(json);
        } catch (Exception e) {
            log.warn("Could not format JSON, saving as-is: {}", e.getMessage());
            return jsonContent;
        }
    }

    private String getJsonFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? originalFileName.substring(0, dotIndex) : originalFileName;
        return nameWithoutExt + ".json";
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
