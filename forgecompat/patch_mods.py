#!/usr/bin/env python3
"""
Patch 1.19.2 mod JARs to work with ForgeCompat on Minecraft 1.20.1

This script modifies the mods.toml file inside 1.19.2 mod JARs to accept
Minecraft 1.20.1, allowing ForgeCompat's transformer to handle them.

Usage:
    python patch_mods.py [mods_directory]
    
If no directory is specified, it defaults to ./mods
"""

import os
import sys
import zipfile
import tempfile
import shutil
import re
from pathlib import Path

def patch_mods_toml(content):
    """Patch Minecraft version ranges in mods.toml content."""
    patched = content
    
    # Pattern 1: versionRange="[1.19.2,1.20)" → "[1.19.2,1.21)"
    patched = re.sub(
        r'versionRange\s*=\s*"\[1\.19\.2,1\.20\)"',
        'versionRange="[1.19.2,1.21)"',
        patched
    )
    
    # Pattern 2: versionRange="[1.19,1.20)" → "[1.19,1.21)"
    patched = re.sub(
        r'versionRange\s*=\s*"\[1\.19,1\.20\)"',
        'versionRange="[1.19,1.21)"',
        patched
    )
    
    # Pattern 3: versionRange="1.19.2" → "[1.19.2,1.21)"
    patched = re.sub(
        r'versionRange\s*=\s*"1\.19\.2"',
        'versionRange="[1.19.2,1.21)"',
        patched
    )
    
    # Pattern 4: versionRange="[1.19.2]" → "[1.19.2,1.21)"
    patched = re.sub(
        r'versionRange\s*=\s*"\[1\.19\.2\]"',
        'versionRange="[1.19.2,1.21)"',
        patched
    )
    
    return patched

def is_1_19_2_mod(content):
    """Check if this mods.toml is for a 1.19.2 mod."""
    return '1.19.2' in content or '[1.19' in content

def patch_jar(jar_path):
    """Patch a single JAR file."""
    try:
        # Check if it's a valid ZIP/JAR
        if not zipfile.is_zipfile(jar_path):
            return False
        
        with zipfile.ZipFile(jar_path, 'r') as zip_ref:
            # Look for mods.toml
            mods_toml_path = None
            for name in zip_ref.namelist():
                if name.endswith('mods.toml'):
                    mods_toml_path = name
                    break
            
            if not mods_toml_path:
                return False
            
            # Read mods.toml
            with zip_ref.open(mods_toml_path) as f:
                content = f.read().decode('utf-8')
            
            # Check if it's a 1.19.2 mod
            if not is_1_19_2_mod(content):
                return False
            
            # Patch the content
            patched_content = patch_mods_toml(content)
            
            if patched_content == content:
                print(f"  [SKIP] {jar_path.name} - no changes needed")
                return False
            
            # Create a temporary file
            temp_path = jar_path.with_suffix('.jar.tmp')
            
            # Write the patched JAR
            with zipfile.ZipFile(jar_path, 'r') as zip_ref:
                with zipfile.ZipFile(temp_path, 'w', zipfile.ZIP_DEFLATED) as zip_out:
                    for item in zip_ref.infolist():
                        if item.filename == mods_toml_path:
                            # Write patched version
                            zip_out.writestr(item, patched_content.encode('utf-8'))
                        else:
                            # Copy as-is
                            zip_out.writestr(item, zip_ref.read(item.filename))
            
            # Replace original with patched
            jar_path.unlink()
            temp_path.rename(jar_path)
            
            print(f"  [PATCHED] {jar_path.name}")
            return True
            
    except Exception as e:
        print(f"  [ERROR] {jar_path.name}: {e}")
        return False

def main():
    # Get mods directory - default to current directory (where the script is)
    if len(sys.argv) > 1:
        mods_dir = Path(sys.argv[1])
    else:
        mods_dir = Path('.')
    
    if not mods_dir.exists():
        print(f"Error: Directory not found: {mods_dir}")
        sys.exit(1)
    
    print(f"Scanning for 1.19.2 mods in: {mods_dir.absolute()}")
    print()
    
    # Find all JAR files
    jar_files = list(mods_dir.glob('*.jar'))
    
    if not jar_files:
        print("No JAR files found in mods directory.")
        sys.exit(0)
    
    print(f"Found {len(jar_files)} JAR file(s)")
    print()
    
    # Patch each JAR
    patched_count = 0
    for jar_file in jar_files:
        if patch_jar(jar_file):
            patched_count += 1
    
    print()
    print(f"Patched {patched_count} mod(s)")
    
    if patched_count > 0:
        print()
        print("You can now launch Minecraft 1.20.1 with ForgeCompat!")
        print("The patched mods will be handled by ForgeCompat's transformer.")

if __name__ == '__main__':
    main()
