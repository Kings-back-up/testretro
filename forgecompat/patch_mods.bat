@echo off
echo ========================================
echo ForgeCompat Mod Patcher
echo ========================================
echo.
echo This will patch 1.19.2 mods to work with
echo ForgeCompat on Minecraft 1.20.1
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python from https://python.org
    pause
    exit /b 1
)

REM Check if mods directory exists
if not exist "mods" (
    echo ERROR: mods directory not found
    echo Please run this script from your Minecraft instance root
    pause
    exit /b 1
)

echo Patching mods in: %CD%\mods
echo.

REM Run the Python patcher
python patch_mods.py mods

echo.
pause
