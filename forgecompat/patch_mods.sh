#!/bin/bash
echo "========================================"
echo "ForgeCompat Mod Patcher"
echo "========================================"
echo ""
echo "This will patch 1.19.2 mods to work with"
echo "ForgeCompat on Minecraft 1.20.1"
echo ""
echo "Place this script in your mods folder"
echo "alongside your 1.19.2 mods, then run it."
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 is not installed or not in PATH"
    echo "Please install Python 3 from https://python.org"
    exit 1
fi

echo "Patching mods in: $(pwd)"
echo ""

# Run the Python patcher on current directory
python3 patch_mods.py .

echo ""
echo "Done! You can now delete this script and patch_mods.py"
echo ""
read -p "Press Enter to continue..."
