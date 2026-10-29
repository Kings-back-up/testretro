# Using ForgeCompat with 1.19.2 Mods

## The Problem

When you try to load a 1.19.2 mod in Minecraft 1.20.1, Forge will reject it because the mod declares it needs Minecraft 1.19.2. This happens before ForgeCompat's transformer even runs.

## The Solution

You need to patch the 1.19.2 mod's `mods.toml` file to accept Minecraft 1.20.1. ForgeCompat provides a script to do this automatically.

## Step-by-Step Instructions

### 1. Install Python (if not already installed)

Download Python from https://python.org (version 3.6 or newer)

During installation, make sure to check "Add Python to PATH"

### 2. Place ForgeCompat and the patcher in your mods folder

Put these files in your modpack's `mods/` folder:
- `forgecompat-0.1.0.jar`
- `patch_mods.py`
- `patch_mods.bat` (Windows) or `patch_mods.sh` (Linux/Mac)
- Your 1.19.2 mod JAR(s)

### 3. Run the patcher

**Windows:**
```
Double-click patch_mods.bat in the mods folder
```

**Linux/Mac:**
```bash
cd /path/to/modpack/mods
chmod +x patch_mods.sh
./patch_mods.sh
```

**Manual (any OS):**
```bash
cd /path/to/modpack/mods
python patch_mods.py .
```

### 4. Delete the patcher files

After patching, you can delete:
- `patch_mods.py`
- `patch_mods.bat` or `patch_mods.sh`

They're only needed once per mod.

### 5. Launch Minecraft

The 1.19.2 mod should now load, and ForgeCompat's transformer will handle the bytecode translation

**Note:** This works with CurseForge, Prism Launcher, MultiMC, and any other launcher because everything is in the mods folder itself.

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
- **Works with any launcher** (CurseForge, Prism, MultiMC, etc.) because everything is in the mods folder

## Troubleshooting

### "Python is not installed"
Install Python from https://python.org and make sure to add it to PATH

### "No JAR files found"
Make sure you're running the script from inside your mods folder (where your mod JARs are)

### Mod still doesn't load after patching
Check the logs for `[ForgeCompat]` messages. The transformer might be missing translation rules for APIs used by that mod.

### "Error: Directory not found"
Make sure you're in the correct directory. Use `cd` to navigate to your mods folder first:
- CurseForge: `cd %APPDATA%\curseforge\minecraft\Instances\[instance-name]\mods`
- Prism/MultiMC: `cd /path/to/instance/mods`
- Vanilla: `cd %APPDATA%\.minecraft\mods`

## Alternative: Manual Patching

If you don't want to use the script, you can manually edit the mod's `mods.toml`:

1. Open the 1.19.2 mod JAR (in your mods folder) with a ZIP editor (like 7-Zip)
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
- A GUI tool for managing patched mods
- Better integration with mod loaders
- Automatic detection of which mods need patching

For now, the patcher script is the most reliable solution and works with all launchers.
