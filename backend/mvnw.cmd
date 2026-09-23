@echo off
setlocal

set "THIS_DIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%THIS_DIR%.mvn\wrapper\maven-wrapper.properties"

:: Check Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo   ERROR: Java no encontrado.
    echo   Instala Java 21 desde https://adoptium.net/
    echo.
    exit /b 1
)

:: Use local Maven cache if present, otherwise try system mvn
set "MVN_CMD=%THIS_DIR%.mvn\wrapper\apache-maven\bin\mvn.cmd"
if not exist "%MVN_CMD%" (
    where mvn >nul 2>&1
    if %errorlevel% equ 0 (
        set "MVN_CMD=mvn"
    ) else (
        echo.
        echo   Maven no encontrado. Opciones:
        echo   1. Instala Maven: https://maven.apache.org/download.cgi
        echo   2. O ejecuta desde Unix/WSL usando ./mvnw
        echo.
        exit /b 1
    )
)

"%MVN_CMD%" %*
