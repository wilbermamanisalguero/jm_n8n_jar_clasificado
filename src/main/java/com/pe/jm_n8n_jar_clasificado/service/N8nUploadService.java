package com.pe.jm_n8n_jar_clasificado.service;

import com.pe.jm_n8n_jar_clasificado.config.AppProperties;
import com.pe.jm_n8n_jar_clasificado.model.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * Service for uploading files to N8N webhook using multipart/form-data.
 * Uses RestTemplate for HTTP operations with timeout configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class N8nUploadService {

    private final AppProperties properties;
    private final RestTemplate restTemplate;

    public UploadResult uploadFile(File file) {
        try {
            log.info("Uploading file to N8N: {}", file.getName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(properties.getMultipartFieldName(), new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getN8nUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            HttpStatusCode statusCode = response.getStatusCode();
            String responseBody = response.getBody();
            log.info("Upload response for {}: HTTP {}", file.getName(), statusCode.value());
            log.debug("Response body: {}", responseBody);

            if (statusCode == HttpStatus.OK || statusCode == HttpStatus.CREATED) {
                log.info("Successfully uploaded file: {}", file.getName());
                return new UploadResult(true, responseBody);
            } else {
                log.warn("Unexpected status code {} for file: {}", statusCode.value(), file.getName());
                return new UploadResult(false, responseBody);
            }

        } catch (RestClientException e) {
            log.error("Failed to upload file {}: {}", file.getName(), e.getMessage(), e);
            return new UploadResult(false, null);
        } catch (Exception e) {
            log.error("Unexpected error uploading file {}: {}", file.getName(), e.getMessage(), e);
            return new UploadResult(false, null);
        }
    }
}
