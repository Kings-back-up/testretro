@echo off
echo ========================================
echo ForgeCompat Mod Patcher
echo ========================================
echo.
echo This will patch 1.19.2 mods to work with
echo ForgeCompat on Minecraft 1.20.1
echo.
echo Place this script in your mods folder
echo alongside your 1.19.2 mods, then run it.
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python from https://python.org
    pause
    exit /b 1
)

echo Patching mods in: %CD%
echo.

REM Run the Python patcher on current directory
python patch_mods.py .

echo.
echo Done! You can now delete this script and patch_mods.py
echo.
pause
