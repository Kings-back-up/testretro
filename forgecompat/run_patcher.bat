@echo off
echo ========================================
echo ForgeCompat Java Patcher
echo ========================================
echo.
echo This will patch 1.19.2 mods to work with
echo ForgeCompat on Minecraft 1.20.1
echo.
echo Place this JAR in your mods folder with
echo your 1.19.2 mods, then run this script.
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Java is required to run this patcher
    pause
    exit /b 1
)

echo Patching mods in: %CD%
echo.

REM Run the Java patcher on current directory
java -jar forgecompat-patcher.jar .

echo.
echo Done! You can now delete this script and the patcher JAR.
echo.
pause
