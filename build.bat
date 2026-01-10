@echo off
REM Script para compilar el proyecto

echo ============================================
echo Compilando proyecto JM N8N Clasificados
echo ============================================
echo.

call mvn clean package

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo BUILD EXITOSO
    echo ============================================
    echo.
    echo JAR generado en: target\jm_n8n_jar_clasificado-0.0.1-SNAPSHOT.jar
    echo.
    echo Para ejecutar: run.bat
    echo.
) else (
    echo.
    echo ============================================
    echo BUILD FALLIDO
    echo ============================================
    echo.
    echo Revisa los errores anteriores
    echo.
)

pause
