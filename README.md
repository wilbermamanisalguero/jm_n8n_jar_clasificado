# JM N8N Clasificados - Servicio de Procesamiento de Imágenes

Servicio Spring Boot que procesa automáticamente imágenes JPG de una carpeta local y las envía a un webhook de N8N como multipart/form-data.

## Características

- Ejecuta un job programado cada 1 minuto
- Procesa archivos `.jpg` con formato de nombre específico: `YYYYMMDD_HHMMSS.jpg`
- Selecciona el archivo más antiguo basándose en el timestamp del nombre
- Envía archivos al webhook de N8N usando multipart/form-data
- Post-procesa archivos según resultado: DELETE o MOVE
- Evita procesamiento de archivos incompletos
- Control de concurrencia para evitar procesamiento duplicado
- Configuración flexible mediante `application.yml`

## Requisitos

- Java 21
- Maven 3.6+
- Windows (configurado para rutas de Windows)

## Stack Tecnológico

- **Spring Boot 4.0.0** (parent)
- **Java 21**
- **Vert.x 4.5.1** (core, mysql-client, sql-client)
- **Lombok** - Reducción de boilerplate
- **Jackson** - Serialización JSON
- **RestTemplate** - Cliente HTTP para multipart upload

### Justificación de RestTemplate

Se eligió `RestTemplate` sobre otras alternativas porque:

1. **Spring Boot Starter Web**: Ya incluido en el POM, no requiere dependencias adicionales
2. **Simplicidad**: API síncrona ideal para jobs programados
3. **Multipart nativo**: Soporte completo para `MultiValueMap` y `FileSystemResource`
4. **Configuración timeout**: Fácil configuración mediante `SimpleClientHttpRequestFactory`

Alternativas consideradas:
- **WebClient**: Requeriría añadir `spring-boot-starter-webflux` (no en el POM original)
- **Apache HttpClient**: Dependencia externa adicional
- **OkHttp**: Dependencia externa adicional

## Estructura del Proyecto

```
src/main/java/com/pe/jm_n8n_jar_clasificado/
├── JmN8nJarClasificadoApplication.java    # Clase principal con @EnableScheduling
├── config/
│   ├── AppProperties.java                  # Configuración con @ConfigurationProperties
│   └── RestTemplateConfig.java             # Bean RestTemplate con timeouts
├── model/
│   └── FileInfo.java                       # Modelo que encapsula File + timestamp
├── scheduler/
│   └── ImageProcessorScheduler.java        # Job programado (cron cada minuto)
└── service/
    ├── FileScannerService.java             # Busca y selecciona archivo más antiguo
    ├── N8nUploadService.java               # Envía archivo al webhook N8N
    └── FilePostProcessService.java         # Procesa archivo post-envío (delete/move)
```

## Configuración

Editar `src/main/resources/application.yml`:

```yaml
app:
  # Carpeta de entrada (REQUERIDO)
  input-folder: C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024

  # URL webhook N8N (REQUERIDO)
  n8n-url: https://wilbermamanisalguero.app.n8n.cloud/webhook-test/4178ec1e-4e41-4719-9fb6-211fbca9b7eb

  # Acción en caso de éxito: DELETE o MOVE (REQUERIDO)
  success-action: MOVE

  # Carpeta de procesados (REQUERIDO si success-action=MOVE)
  processed-folder: C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024\processed

  # Carpeta de errores (OPCIONAL)
  error-folder: C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024\error

  # Edad mínima del archivo en segundos (evita archivos copiándose)
  min-file-age-seconds: 5

  # Timeout HTTP en segundos
  timeout-seconds: 30

  # Reintentos máximos (reservado para futuro)
  max-retries: 3

  # Nombre del campo multipart
  multipart-field-name: clasificadoImgN8n
```

### Parámetros de Configuración

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `input-folder` | String | Sí | Carpeta donde se buscan los archivos JPG |
| `n8n-url` | String | Sí | URL del webhook de N8N |
| `success-action` | Enum | Sí | `DELETE` o `MOVE` |
| `processed-folder` | String | Condicional | Requerido si `success-action=MOVE` |
| `error-folder` | String | No | Si se especifica, archivos con error se mueven aquí |
| `min-file-age-seconds` | int | No | Default: 5. Edad mínima del archivo |
| `timeout-seconds` | int | No | Default: 30. Timeout de conexión y lectura |
| `max-retries` | int | No | Default: 3. Reservado para implementación futura |
| `multipart-field-name` | String | No | Default: `clasificadoImgN8n` |

## Formato de Archivos

El servicio **solo procesa** archivos con este formato exacto:

```
YYYYMMDD_HHMMSS.jpg
```

Ejemplos válidos:
- `20251228_151333.jpg`
- `20240101_093045.jpg`
- `20251231_235959.jpg`

Ejemplos inválidos (serán ignorados):
- `20251228_151333.jpeg` (extensión incorrecta)
- `20251228_151333.JPG` (mayúsculas)
- `foto_20251228.jpg` (formato incorrecto)
- `20251228.jpg` (falta hora)
- `IMG_1234.jpg` (no cumple patrón)

## Compilación

```bash
mvn clean package
```

Esto generará el archivo JAR en `target/jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar`

## Ejecución en Windows

### Opción 1: Maven

```bash
mvn spring-boot:run
```

### Opción 2: JAR (Recomendado para producción)

```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar
```

### Opción 3: Con configuración personalizada

```bash
java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar ^
  --app.input-folder=D:\MiCarpeta\Imagenes ^
  --app.success-action=DELETE
```

## Funcionamiento

### Flujo de Procesamiento

1. **Cada minuto** (cron: `0 * * * * *`), el scheduler se activa
2. **Escanea** la carpeta `input-folder` buscando archivos `*.jpg`
3. **Valida** que cumplan el patrón `YYYYMMDD_HHMMSS.jpg`
4. **Filtra** archivos:
   - Tamaño > 0 bytes
   - Edad > `min-file-age-seconds` (evita archivos incompletos)
5. **Selecciona** el archivo más antiguo según timestamp del nombre
6. **Envía** el archivo al webhook N8N como multipart/form-data
7. **Post-procesa** según resultado:
   - **200/201 OK**: Ejecuta `success-action` (DELETE o MOVE a `processed-folder`)
   - **Error**: Mueve a `error-folder` (si está configurado)

### Logs

El servicio genera logs informativos en cada ejecución:

```
2025-01-09 14:32:00 - === Starting scheduled image processing job ===
2025-01-09 14:32:00 - Selected file for processing: 20251228_135619.jpg (timestamp: 2025-12-28T13:56:19)
2025-01-09 14:32:00 - Uploading file to N8N: 20251228_135619.jpg
2025-01-09 14:32:01 - Upload response for 20251228_135619.jpg: HTTP 200
2025-01-09 14:32:01 - Successfully uploaded file: 20251228_135619.jpg
2025-01-09 14:32:01 - Upload successful for file: 20251228_135619.jpg
2025-01-09 14:32:01 - Moved file 20251228_135619.jpg to processed folder successfully
2025-01-09 14:32:01 - === Completed scheduled image processing job ===
```

Si no hay archivos pendientes:

```
2025-01-09 14:33:00 - === Starting scheduled image processing job ===
2025-01-09 14:33:00 - No pending files to process
2025-01-09 14:33:00 - === Completed scheduled image processing job ===
```

## Control de Concurrencia

El scheduler usa un `AtomicBoolean` para evitar ejecuciones simultáneas:

- Si una ejecución aún está en curso cuando llega el siguiente minuto, se omite
- Log: `"Previous job execution still running, skipping this iteration"`
- Garantiza que **solo un archivo se procesa a la vez**

## Evitar Archivos Incompletos

Dos mecanismos protegen contra archivos a medio copiar:

1. **Tamaño cero**: Archivos de 0 bytes se ignoran
2. **Edad del archivo**: Solo procesa archivos cuyo `lastModified` sea mayor a `min-file-age-seconds`

Ejemplo: Si `min-file-age-seconds=5`, solo procesa archivos que no se hayan modificado en los últimos 5 segundos.

## Equivalente cURL

El servicio realiza lo equivalente a:

```bash
curl --location 'https://wilbermamanisalguero.app.n8n.cloud/webhook-test/4178ec1e-4e41-4719-9fb6-211fbca9b7eb' \
  --form 'clasificadoImgN8n=@"C:/Users/wmamani/Downloads/Trabajo_JmAlpaca_2022-2024/20251228_135619.jpg"'
```

## Escenarios de Uso

### Escenario 1: Borrar archivos procesados

```yaml
app:
  success-action: DELETE
  error-folder: C:\...\error  # Opcional, para mantener evidencia de errores
```

### Escenario 2: Mover archivos procesados (Recomendado)

```yaml
app:
  success-action: MOVE
  processed-folder: C:\...\processed
  error-folder: C:\...\error
```

Este escenario es más seguro porque:
- Mantiene evidencia de todos los archivos procesados
- Permite auditoría posterior
- Facilita reprocesamiento si es necesario

## Consideraciones de Producción

1. **Crear carpetas manualmente** (opcional):
   ```bash
   mkdir C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024\processed
   mkdir C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024\error
   ```
   (El servicio las crea automáticamente si no existen)

2. **Verificar permisos** de escritura en carpetas `processed` y `error`

3. **Monitorear logs** para detectar errores de red o configuración

4. **Ajustar `timeout-seconds`** según latencia de red al webhook N8N

5. **Ajustar `min-file-age-seconds`** según velocidad de copia de archivos

## Troubleshooting

### El servicio no procesa archivos

1. Verificar que los archivos cumplan el patrón `YYYYMMDD_HHMMSS.jpg`
2. Verificar que `input-folder` sea la ruta correcta
3. Verificar que el archivo tenga edad > `min-file-age-seconds`
4. Revisar logs para mensajes de error

### Error "Processed folder not configured"

Si `success-action=MOVE` pero `processed-folder` está vacío, el servicio no puede mover archivos.

Solución: Configurar `processed-folder` en `application.yml`

### Error de conexión al webhook

- Verificar conectividad de red
- Verificar que la URL del webhook sea correcta
- Aumentar `timeout-seconds` si la red es lenta

### Archivos quedan en carpeta input

Posibles causas:
- Error HTTP (verificar logs)
- `error-folder` no configurado y `success-action=MOVE`
- Permisos insuficientes para borrar/mover archivos

## Desarrollo Futuro

Características no implementadas pero contempladas en configuración:

- **`max-retries`**: Reintentos automáticos antes de marcar como error
- **Base de datos**: Usar Vert.x MySQL Client para persistir historial de procesamiento
- **API REST**: Endpoint para consultar estado/historial

## Licencia

Proyecto demo para procesamiento de imágenes clasificadas JM.
