# Using ForgeCompat with 1.19.2 Mods

## The Problem

When you try to load a 1.19.2 mod in Minecraft 1.20.1, Forge will reject it because the mod declares it needs Minecraft 1.19.2. This happens before ForgeCompat's transformer even runs.

## The Solution

You need to patch the 1.19.2 mod's `mods.toml` file to accept Minecraft 1.20.1. ForgeCompat provides a script to do this automatically.

## Step-by-Step Instructions

### 1. Install Python (if not already installed)

Download Python from https://python.org (version 3.6 or newer)

During installation, make sure to check "Add Python to PATH"

### 2. Place ForgeCompat in your mods folder

Put `forgecompat-0.1.0.jar` in your Minecraft 1.20.1 instance's `mods/` folder

### 3. Place the patch script in your Minecraft root

Copy these files to your Minecraft instance root (the folder that contains the `mods/` directory):
- `patch_mods.py`
- `patch_mods.bat` (Windows) or `patch_mods.sh` (Linux/Mac)

### 4. Run the patcher

**Windows:**
```
Double-click patch_mods.bat
```

**Linux/Mac:**
```bash
chmod +x patch_mods.sh
./patch_mods.sh
```

**Manual (any OS):**
```bash
python patch_mods.py mods
```

### 5. Place your 1.19.2 mod in the mods folder

Now you can put your 1.19.2 mod JAR in the `mods/` folder

### 6. Launch Minecraft

The 1.19.2 mod should now load, and ForgeCompat's transformer will handle the bytecode translation

## What the Patcher Does

The patcher:
1. Scans all JAR files in your `mods/` folder
2. Finds mods that declare Minecraft 1.19.2 compatibility
3. Modifies their `mods.toml` to accept Minecraft 1.20.1
4. Repacks the JAR with the modified metadata

Example change:
```toml
# Before:
versionRange="[1.19.2,1.20)"

# After:
versionRange="[1.19.2,1.21)"
```

## Important Notes

- **Run the patcher BEFORE launching Minecraft** with the 1.19.2 mod
- The patcher only modifies mods that declare 1.19.2 compatibility
- You only need to run the patcher once per mod
- The patched mods will work with ForgeCompat's transformer
- Keep the original 1.19.2 mod JARs backed up in case you need them

## Troubleshooting

### "Python is not installed"
Install Python from https://python.org and make sure to add it to PATH

### "mods directory not found"
Make sure you're running the script from your Minecraft instance root (the folder that contains the `mods/` directory)

### Mod still doesn't load after patching
Check the logs for `[ForgeCompat]` messages. The transformer might be missing translation rules for APIs used by that mod.

### "No JAR files found"
Make sure your 1.19.2 mod JAR is in the `mods/` folder before running the patcher

## Alternative: Manual Patching

If you don't want to use the script, you can manually edit the mod's `mods.toml`:

1. Open the 1.19.2 mod JAR with a ZIP editor (like 7-Zip)
2. Extract `META-INF/mods.toml`
3. Find the Minecraft dependency section:
   ```toml
   [[dependencies.modid]]
       modId="minecraft"
       mandatory=true
       versionRange="[1.19.2,1.20)"
   ```
4. Change `versionRange` to `"[1.19.2,1.21)"`
5. Save the file and put it back in the JAR

## Future Improvements

In the future, ForgeCompat might include:
- Automatic patching on first launch
- A GUI tool for managing patched mods
- Better integration with mod loaders

For now, the patcher script is the most reliable solution.
