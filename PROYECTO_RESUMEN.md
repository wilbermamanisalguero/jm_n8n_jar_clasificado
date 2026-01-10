# Resumen del Proyecto - JM N8N Clasificados

## Estructura de Archivos Creados

### Configuración Maven
- **pom.xml** - Configurado con Spring Boot 4.0.0, Java 21, Vert.x 4.5.1 y todas las dependencias requeridas

### Código Java

#### Paquete `config/`
1. **AppProperties.java** - Clase de configuración con `@ConfigurationProperties` que mapea todas las propiedades del `application.yml`
   - Validaciones con Bean Validation
   - Enum `SuccessAction` (DELETE/MOVE)

2. **RestTemplateConfig.java** - Configuración del bean `RestTemplate` con timeouts configurables

#### Paquete `model/`
3. **FileInfo.java** - Modelo simple que encapsula un `File` con su `LocalDateTime` parseado del nombre

#### Paquete `service/`
4. **FileScannerService.java** - Servicio que escanea la carpeta y encuentra el archivo más antiguo
   - Valida patrón `YYYYMMDD_HHMMSS.jpg`
   - Filtra archivos vacíos
   - Filtra archivos muy recientes (evita procesar archivos incompletos)
   - Devuelve el más antiguo según timestamp del nombre

5. **N8nUploadService.java** - Servicio que sube archivos al webhook N8N
   - Usa `RestTemplate` con multipart/form-data
   - Maneja timeouts configurables
   - Retorna true/false según respuesta HTTP

6. **FilePostProcessService.java** - Servicio que procesa archivos después del envío
   - DELETE: Borra el archivo
   - MOVE: Mueve a carpeta `processed/` o `error/`
   - Crea carpetas automáticamente si no existen
   - Maneja conflictos de nombres con timestamp único

#### Paquete `scheduler/`
7. **ImageProcessorScheduler.java** - Job programado que se ejecuta cada minuto
   - Cron: `0 * * * * *`
   - Control de concurrencia con `AtomicBoolean`
   - Orquesta todo el flujo: scan → upload → post-process

#### Raíz
8. **JmN8nJarClasificadoApplication.java** - Clase principal con `@EnableScheduling`

### Recursos

9. **application.yml** - Configuración por defecto con valores de ejemplo
10. **application-dev.yml** - Perfil de desarrollo con valores alternativos

### Documentación

11. **README.md** - Documentación completa del proyecto con:
    - Características
    - Requisitos
    - Justificación técnica
    - Instrucciones de compilación y ejecución
    - Configuración detallada
    - Troubleshooting

12. **PROYECTO_RESUMEN.md** (este archivo) - Resumen de estructura

### Scripts

13. **build.bat** - Script para compilar el proyecto en Windows
14. **run.bat** - Script para ejecutar el JAR en Windows

### Control de Versiones

15. **.gitignore** - Actualizado con carpetas específicas del proyecto (`processed/`, `error/`)

## Características Implementadas

### 1. Scheduler (cada 1 minuto)
- `@Scheduled(cron = "0 * * * * *")`
- Control de concurrencia para evitar ejecuciones simultáneas

### 2. Validación de Archivos
- Patrón exacto: `YYYYMMDD_HHMMSS.jpg`
- Regex: `^(\d{8})_(\d{6})\.jpg$`
- Ignora archivos de tamaño 0
- Respeta edad mínima configurable

### 3. Selección Inteligente
- Elige archivo más antiguo por timestamp del **nombre** (no lastModified)
- Comparación directa de `LocalDateTime`

### 4. Upload HTTP Multipart
- `RestTemplate` con configuración de timeout
- Campo configurable: `clasificadoImgN8n`
- Detecta respuesta 200/201 como éxito

### 5. Post-Procesamiento Configurable
- **DELETE**: Borra el archivo directamente
- **MOVE**: Mueve a carpeta `processed/`
- **ERROR**: Mueve a carpeta `error/` (opcional)
- Creación automática de carpetas
- Resolución de conflictos de nombres

### 6. Logging Completo
- Nivel INFO para producción
- Nivel DEBUG disponible en perfil dev
- Logs claros en cada paso del proceso

## Flujo de Ejecución

```
┌─────────────────────────────────────────────────────┐
│  CADA MINUTO (cron: 0 * * * * *)                    │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  ImageProcessorScheduler.processNextImage()         │
│  - Verifica lock de concurrencia                    │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│  FileScannerService.findOldestValidFile()           │
│  - Lista archivos *.jpg                             │
│  - Valida patrón YYYYMMDD_HHMMSS.jpg                │
│  - Filtra tamaño > 0                                │
│  - Filtra edad > minFileAgeSeconds                  │
│  - Retorna el más antiguo                           │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
        ¿Archivo encontrado?
         /              \
       NO                SÍ
        │                 │
        ▼                 ▼
    Log "No         N8nUploadService.uploadFile()
    pending"        - POST multipart/form-data
        │            - Timeout configurable
        │                 │
        │                 ▼
        │         ¿HTTP 200/201?
        │          /            \
        │        SÍ             NO
        │         │              │
        │         ▼              ▼
        │   processSuccessful   processFailedUpload()
        │   Upload()             - Mueve a error/
        │   - DELETE o MOVE     - Log error
        │   - Log éxito
        │         │              │
        └─────────┴──────────────┘
                  │
                  ▼
              Fin del job
```

## Configuración Mínima Requerida

```yaml
app:
  input-folder: C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024
  n8n-url: https://wilbermamanisalguero.app.n8n.cloud/webhook-test/4178ec1e-4e41-4719-9fb6-211fbca9b7eb
  success-action: MOVE
  processed-folder: C:\Users\wmamani\Downloads\Trabajo_JmAlpaca_2022-2024\processed
```

## Decisiones Técnicas

### ¿Por qué RestTemplate?

1. **Incluido en spring-boot-starter-web** - No requiere dependencias extra
2. **API síncrona** - Perfecta para jobs programados (no necesitamos reactividad)
3. **Soporte nativo de multipart** - `LinkedMultiValueMap` + `FileSystemResource`
4. **Configuración simple de timeout** - `SimpleClientHttpRequestFactory`

Alternativas descartadas:
- **WebClient**: Requeriría `spring-boot-starter-webflux` (no en POM original)
- **Apache HttpClient**: Dependencia externa adicional
- **OkHttp**: Dependencia externa adicional

### ¿Por qué AtomicBoolean para concurrencia?

- **Simplicidad**: No requiere sincronización compleja
- **Suficiente**: Solo necesitamos evitar doble ejecución
- **Bajo overhead**: Operaciones atómicas de bajo costo

Alternativa considerada:
- **ReentrantLock**: Más pesado, innecesario para este caso simple

### ¿Por qué validar edad del archivo?

Evita procesar archivos que aún se están copiando a la carpeta:
- Un archivo grande puede tomar varios segundos en copiarse
- Si el scheduler lo detecta mientras se copia, podría enviarse incompleto
- `minFileAgeSeconds=5` asegura estabilidad

## Testing

Para probar el servicio:

1. Crear archivos de prueba:
   ```bash
   # Formato correcto
   20251209_140530.jpg
   20251209_140645.jpg
   20251209_140712.jpg

   # Formato incorrecto (serán ignorados)
   imagen.jpg
   20251209.jpg
   ```

2. Iniciar el servicio
3. Observar logs cada minuto
4. Verificar que procesa el más antiguo primero
5. Verificar que archivos se mueven/eliminan correctamente

## Métricas de Complejidad

- **Clases Java**: 8
- **Líneas de código**: ~600 (sin contar comentarios)
- **Configuraciones**: 2 (application.yml + application-dev.yml)
- **Dependencias Maven**: 11
- **Patrones usados**: Service Layer, Configuration Properties, Scheduled Tasks, Dependency Injection

## Próximos Pasos (Opcionales)

1. **Sistema de reintentos**: Implementar `maxRetries` con backoff exponencial
2. **Base de datos**: Usar Vert.x MySQL Client para persistir historial
3. **Métricas**: Spring Boot Actuator para monitoreo
4. **API REST**: Endpoint para consultar estado/historial
5. **Validación de imágenes**: Verificar que realmente sean JPG válidos
6. **Notificaciones**: Email/Slack en caso de errores persistentes
