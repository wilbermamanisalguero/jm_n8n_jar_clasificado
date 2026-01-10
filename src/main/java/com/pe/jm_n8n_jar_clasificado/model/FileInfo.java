package com.pe.jm_n8n_jar_clasificado.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FileInfo {
    private File file;
    private LocalDateTime timestamp;
}
