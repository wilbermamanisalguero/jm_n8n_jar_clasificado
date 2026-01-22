package com.pe.jm_n8n_jar_clasificado.service;

import com.pe.jm_n8n_jar_clasificado.config.AppProperties;
import com.pe.jm_n8n_jar_clasificado.model.FileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScannerService {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^(\\d{8})_(\\d{6})\\.png$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AppProperties properties;

    public Optional<FileInfo> findOldestValidFile() {
        File folder = new File(properties.getInputFolder());

        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Input folder does not exist or is not a directory: {}", properties.getInputFolder());
            return Optional.empty();
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            log.debug("No .png files found in folder: {}", properties.getInputFolder());
            return Optional.empty();
        }

        return Arrays.stream(files)
                .map(this::parseFileInfo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isFileReadyToProcess)
                .min(Comparator.comparing(FileInfo::getTimestamp));
    }

    private Optional<FileInfo> parseFileInfo(File file) {
        String filename = file.getName();
        Matcher matcher = FILENAME_PATTERN.matcher(filename);

        if (!matcher.matches()) {
            log.trace("File does not match expected pattern: {}", filename);
            return Optional.empty();
        }

        try {
            String dateTimePart = matcher.group(1) + "_" + matcher.group(2);
            LocalDateTime timestamp = LocalDateTime.parse(dateTimePart, FORMATTER);

            if (file.length() == 0) {
                log.debug("Skipping empty file: {}", filename);
                return Optional.empty();
            }

            return Optional.of(new FileInfo(file, timestamp));
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse timestamp from filename: {}", filename, e);
            return Optional.empty();
        }
    }

    private boolean isFileReadyToProcess(FileInfo fileInfo) {
        File file = fileInfo.getFile();
        long lastModified = file.lastModified();
        long currentTime = System.currentTimeMillis();
        long ageInSeconds = (currentTime - lastModified) / 1000;

        if (ageInSeconds < properties.getMinFileAgeSeconds()) {
            log.debug("File {} is too recent ({}s old), waiting for stability (min: {}s)",
                    file.getName(), ageInSeconds, properties.getMinFileAgeSeconds());
            return false;
        }

        return true;
    }
}
