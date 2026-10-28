#!/bin/bash
echo "========================================"
echo "ForgeCompat Mod Patcher"
echo "========================================"
echo ""
echo "This will patch 1.19.2 mods to work with"
echo "ForgeCompat on Minecraft 1.20.1"
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 is not installed or not in PATH"
    echo "Please install Python 3 from https://python.org"
    exit 1
fi

# Check if mods directory exists
if [ ! -d "mods" ]; then
    echo "ERROR: mods directory not found"
    echo "Please run this script from your Minecraft instance root"
    exit 1
fi

echo "Patching mods in: $(pwd)/mods"
echo ""

# Run the Python patcher
python3 patch_mods.py mods

echo ""
read -p "Press Enter to continue..."
