package com.pe.jm_n8n_jar_clasificado.scheduler;

import com.pe.jm_n8n_jar_clasificado.model.FileInfo;
import com.pe.jm_n8n_jar_clasificado.model.UploadResult;
import com.pe.jm_n8n_jar_clasificado.service.FilePostProcessService;
import com.pe.jm_n8n_jar_clasificado.service.FileScannerService;
import com.pe.jm_n8n_jar_clasificado.service.N8nUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
 
import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessorScheduler {

    private final FileScannerService fileScannerService;
    private final N8nUploadService uploadService;
    private final FilePostProcessService postProcessService;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
 
    /**
     * Scheduled job that runs every minute.
     * Processes the oldest valid .png file from the input folder.
     */
    @Scheduled(fixedDelay = 90000)
    public void processNextImage() {
        if (!isProcessing.compareAndSet(false, true)) {
            log.warn("Previous job execution still running, skipping this iteration");
            return;
        }

        try {
            log.info("=== Starting scheduled image processing job ===");

            Optional<FileInfo> fileInfoOpt = fileScannerService.findOldestValidFile();

            if (fileInfoOpt.isEmpty()) {
                log.info("No pending files to process");
                return;
            }

            FileInfo fileInfo = fileInfoOpt.get();
            File file = fileInfo.getFile();

            log.info("Selected file for processing: {} (timestamp: {})",
                    file.getName(), fileInfo.getTimestamp());

            UploadResult uploadResult = uploadService.uploadFile(file);

            if (uploadResult.isSuccess()) {
                log.info("Upload successful for file: {}", file.getName());
                postProcessService.processSuccessfulUpload(file, uploadResult.getJsonResponse());
            } else {
                log.error("Upload failed for file: {}", file.getName());
                postProcessService.processFailedUpload(file, uploadResult.getJsonResponse());
            }

        } catch (Exception e) {
            log.error("Unexpected error during scheduled job execution", e);
        } finally {
            isProcessing.set(false);
            log.info("=== Completed scheduled image processing job ===");
        }
    }
}
