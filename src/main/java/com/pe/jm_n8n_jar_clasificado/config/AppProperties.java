package com.pe.jm_n8n_jar_clasificado.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "Input folder must be specified")
    private String inputFolder;

    @NotBlank(message = "N8N URL must be specified")
    private String n8nUrl;

    @NotNull(message = "Success action must be specified")
    private SuccessAction successAction;

    private String processedFolder;

    private String errorFolder;

    @Min(value = 0, message = "Min file age seconds must be >= 0")
    private int minFileAgeSeconds = 5;

    @Min(value = 1, message = "Timeout seconds must be >= 1")
    private int timeoutSeconds = 30;

    @Min(value = 0, message = "Max retries must be >= 0")
    private int maxRetries = 3;

    private String multipartFieldName = "clasificadoImgN8n";

    public enum SuccessAction {
        DELETE,
        MOVE
    }
}
