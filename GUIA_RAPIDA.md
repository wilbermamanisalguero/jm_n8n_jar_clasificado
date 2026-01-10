# Guía Rápida de Uso

## Inicio Rápido (Windows)

### 1. Compilar el Proyecto

```bash
# Opción 1: Usar script
build.bat

# Opción 2: Maven directo
mvn clean package
```

### 2. Configurar application.yml

Editar `src\main\resources\application.yml` y ajustar:

```yaml
app:
  input-folder: C:\TU_CARPETA_AQUI
  n8n-url: TU_WEBHOOK_URL_AQUI
  success-action: MOVE  # o DELETE
  processed-folder: C:\TU_CARPETA_AQUI\processed
```

### 3. Ejecutar

```bash
# Opción 1: Usar script
run.bat

# Opción 2: JAR directo
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar
```

### 4. Observar Logs

El servicio mostrará logs cada minuto:

```
2025-01-09 14:32:00 - === Starting scheduled image processing job ===
2025-01-09 14:32:00 - Selected file for processing: 20251228_135619.jpg
2025-01-09 14:32:01 - Successfully uploaded file: 20251228_135619.jpg
2025-01-09 14:32:01 - Moved file to processed folder successfully
2025-01-09 14:32:01 - === Completed scheduled image processing job ===
```

## Casos de Uso Comunes

### Caso 1: Procesar y Borrar

**Escenario**: No necesito conservar archivos procesados.

**Configuración**:
```yaml
app:
  success-action: DELETE
  # processed-folder no es necesario
  error-folder: C:\...\error  # Opcional
```

**Resultado**: Archivos exitosos se borran, archivos con error van a `error/`

---

### Caso 2: Procesar y Archivar (Recomendado)

**Escenario**: Quiero mantener registro de todo.

**Configuración**:
```yaml
app:
  success-action: MOVE
  processed-folder: C:\...\processed
  error-folder: C:\...\error
```

**Resultado**:
- Archivos exitosos → `processed/`
- Archivos con error → `error/`
- Carpeta input queda vacía

---

### Caso 3: Desarrollo/Testing

**Escenario**: Estoy probando el servicio con archivos de prueba.

**Usar perfil dev**:
```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**Crear archivos de prueba**:
```bash
mkdir C:\temp\test-images
# Copiar archivos JPG con formato YYYYMMDD_HHMMSS.jpg
```

---

### Caso 4: Cambiar URL del Webhook sin Recompilar

**Usar parámetro en línea de comandos**:
```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar ^
  --app.n8n-url=https://mi-otro-webhook.com/test
```

---

### Caso 5: Cambiar Carpeta sin Recompilar

```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar ^
  --app.input-folder=D:\Otra_Carpeta\Imagenes
```

## Ejemplos de Nombres de Archivo

### Válidos (serán procesados)
```
20251209_140530.jpg
20240101_000000.jpg
20251231_235959.jpg
20220515_123045.jpg
```

### Inválidos (serán ignorados)
```
20251209_140530.JPG         # Mayúsculas
20251209_140530.jpeg        # Extensión incorrecta
foto_20251209_140530.jpg    # Prefijo extra
20251209.jpg                # Falta hora
imagen.jpg                  # No cumple patrón
IMG_1234.jpg                # No cumple patrón
```

## Logs y Diagnóstico

### Log Normal (Sin Archivos)
```
=== Starting scheduled image processing job ===
No pending files to process
=== Completed scheduled image processing job ===
```

### Log Normal (Con Archivo)
```
=== Starting scheduled image processing job ===
Selected file for processing: 20251228_135619.jpg (timestamp: 2025-12-28T13:56:19)
Uploading file to N8N: 20251228_135619.jpg
Upload response for 20251228_135619.jpg: HTTP 200
Successfully uploaded file: 20251228_135619.jpg
Upload successful for file: 20251228_135619.jpg
Moved file 20251228_135619.jpg to processed folder successfully
=== Completed scheduled image processing job ===
```

### Log de Error (Archivo Muy Reciente)
```
=== Starting scheduled image processing job ===
File 20251209_140530.jpg is too recent (2s old), waiting for stability (min: 5s)
No pending files to process
=== Completed scheduled image processing job ===
```

### Log de Error (Fallo HTTP)
```
=== Starting scheduled image processing job ===
Selected file for processing: 20251228_135619.jpg
Uploading file to N8N: 20251228_135619.jpg
Failed to upload file 20251228_135619.jpg: Connection timeout
Upload failed for file: 20251228_135619.jpg
Moved file 20251228_135619.jpg to error folder successfully
=== Completed scheduled image processing job ===
```

### Log de Advertencia (Ejecución Concurrente)
```
Previous job execution still running, skipping this iteration
```

## Troubleshooting Rápido

### Problema: No procesa ningún archivo

**Posibles causas**:
1. Archivos no cumplen patrón `YYYYMMDD_HHMMSS.jpg`
2. Carpeta `input-folder` incorrecta
3. Archivos muy recientes (< `minFileAgeSeconds`)

**Solución**:
- Verificar nombres de archivos
- Verificar ruta en `application.yml`
- Esperar unos segundos si acabas de copiar archivos

---

### Problema: Error "Processed folder not configured"

**Causa**: `success-action=MOVE` pero `processed-folder` vacío

**Solución**:
```yaml
app:
  processed-folder: C:\tu_carpeta\processed
```

---

### Problema: Error de conexión al webhook

**Causas posibles**:
- Sin internet
- URL incorrecta
- Timeout muy corto

**Soluciones**:
- Verificar conectividad
- Verificar URL en navegador
- Aumentar timeout:
  ```yaml
  app:
    timeout-seconds: 60
  ```

---

### Problema: Archivo se queda en input folder

**Causas posibles**:
- Error HTTP (no 200/201)
- No configurado `error-folder`
- Permisos insuficientes

**Soluciones**:
- Revisar logs para ver código HTTP
- Configurar `error-folder`
- Ejecutar como administrador

---

### Problema: "Previous job still running"

**Causa**: Job anterior tarda más de 1 minuto

**Soluciones**:
- Verificar velocidad de red
- Aumentar `timeout-seconds`
- Verificar tamaño de archivos

## Comandos Útiles

### Ver versión de Java
```bash
java -version
# Debe mostrar: java version "21.x.x"
```

### Verificar JAR generado
```bash
dir target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar
```

### Limpiar target y recompilar
```bash
mvn clean
mvn package
```

### Ejecutar con logs de debug
```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar ^
  --logging.level.com.pe.jm_n8n_jar_clasificado=DEBUG
```

### Ver ayuda de Spring Boot
```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar --help
```

## Configuraciones Avanzadas

### Cambiar cron (ejecutar cada 30 segundos)

Editar `ImageProcessorScheduler.java`:
```java
@Scheduled(cron = "*/30 * * * * *")  // Cada 30 segundos
```

### Cambiar cron (ejecutar cada 5 minutos)

```java
@Scheduled(cron = "0 */5 * * * *")  // Cada 5 minutos
```

### Deshabilitar scheduling temporalmente

```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar ^
  --spring.task.scheduling.enabled=false
```

## Monitoreo

### Verificar que el servicio está corriendo

1. Abrir navegador en `http://localhost:8080` (verá error 404 - es normal)
2. Si el puerto responde, el servicio está activo

### Cambiar puerto si 8080 está ocupado

```yaml
server:
  port: 8081
```

O por línea de comandos:
```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar --server.port=8081
```

## Despliegue en Producción

### Recomendaciones:

1. **Usar success-action: MOVE** (más seguro que DELETE)
2. **Configurar error-folder** (mantener evidencia)
3. **Ajustar min-file-age-seconds** según tamaño de archivos
4. **Monitorear carpeta error/** periódicamente
5. **Logs**: Redirigir a archivo
   ```bash
   java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar > logs.txt 2>&1
   ```

### Ejecutar como servicio Windows

1. Usar herramientas como:
   - **NSSM** (Non-Sucking Service Manager)
   - **WinSW** (Windows Service Wrapper)

2. Ejemplo con NSSM:
   ```bash
   nssm install JmN8nClasificados "java.exe" "-jar C:\ruta\target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar"
   nssm start JmN8nClasificados
   ```

## Testing Manual

### Script de prueba (crear archivos de ejemplo)

Crear archivo `crear_archivos_prueba.bat`:

```batch
@echo off
setlocal enabledelayedexpansion

set FOLDER=C:\temp\test-images
mkdir %FOLDER% 2>nul

REM Crear 5 archivos JPG de prueba con nombres válidos
echo. > %FOLDER%\20240101_120000.jpg
echo. > %FOLDER%\20240101_120100.jpg
echo. > %FOLDER%\20240101_120200.jpg
echo. > %FOLDER%\20240101_120300.jpg
echo. > %FOLDER%\20240101_120400.jpg

echo Archivos de prueba creados en %FOLDER%
dir %FOLDER%\*.jpg
pause
```

**Nota**: Estos archivos estarán vacíos. Para pruebas reales, copiar JPG reales.

## Preguntas Frecuentes

**Q: ¿Puedo cambiar el nombre del campo multipart?**
A: Sí, en `application.yml`:
```yaml
app:
  multipart-field-name: miCampo
```

**Q: ¿Puedo procesar otros formatos además de JPG?**
A: Requiere modificar el código en `FileScannerService.java` (cambiar `.jpg` y regex)

**Q: ¿Cómo detengo el servicio?**
A: Presionar `Ctrl+C` en la consola

**Q: ¿Los archivos se procesan en orden?**
A: Sí, siempre el más antiguo primero según timestamp del nombre

**Q: ¿Qué pasa si dos archivos tienen el mismo timestamp?**
A: Se procesa uno cualquiera (comportamiento no determinístico)

**Q: ¿Puedo ejecutar múltiples instancias?**
A: No recomendado. Podrían procesar el mismo archivo simultáneamente.
