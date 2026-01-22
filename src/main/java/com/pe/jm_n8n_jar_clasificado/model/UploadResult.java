package com.pe.jm_n8n_jar_clasificado.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {
    private boolean success;
    private String jsonResponse;
}
