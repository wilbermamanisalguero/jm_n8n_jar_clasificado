@echo off
REM Script para ejecutar el servicio JM N8N Clasificados
REM Asegúrate de tener Java 21 instalado

echo ============================================
echo JM N8N Clasificados - Image Upload Service
echo ============================================
echo.

REM Verificar si existe el JAR
if not exist "target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar" (
    echo ERROR: JAR no encontrado. Ejecuta primero: mvn clean package
    echo.
    pause
    exit /b 1
)

echo Iniciando servicio...
echo.
echo Presiona Ctrl+C para detener el servicio
echo.

java -jar target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar

pause
