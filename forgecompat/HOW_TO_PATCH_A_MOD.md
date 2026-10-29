# How to Patch a 1.19.2 Mod for ForgeCompat

This guide shows you how to manually edit a 1.19.2 mod to work with ForgeCompat on Minecraft 1.20.1.

**Time required:** 2-3 minutes per mod  
**Difficulty:** Easy  
**Tools needed:** Any ZIP editor (7-Zip, WinRAR, or built-in tools)

## Step-by-Step Instructions

### Step 1: Open the Mod JAR

1. Navigate to your `mods` folder
2. Right-click the 1.19.2 mod JAR file
3. Select "Open with..." and choose your ZIP editor:
   - **Windows:** 7-Zip, WinRAR, or Windows Explorer
   - **Mac:** Double-click to open with Archive Utility
   - **Linux:** File Roller, Ark, or `unzip` command

### Step 2: Find mods.toml

1. Inside the JAR, navigate to the `META-INF` folder
2. Find the file named `mods.toml`
3. Extract it to a temporary location (your desktop works)

### Step 3: Edit mods.toml

1. Open `mods.toml` with any text editor (Notepad, VS Code, etc.)
2. Look for the Minecraft dependency section. It will look like this:

```toml
[[dependencies.your_mod_id]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.19.2,1.20)"
    ordering="NONE"
    side="BOTH"
```

3. **Change the `versionRange` line** from:
   ```
   versionRange="[1.19.2,1.20)"
   ```
   
   **To:**
   ```
   versionRange="[1.19.2,1.21)"
   ```

4. Save the file

### Step 4: Put It Back

1. Go back to your ZIP editor
2. Drag the edited `mods.toml` back into the `META-INF` folder
3. When prompted, choose to **replace/overwrite** the existing file
4. Close the ZIP editor

### Step 5: Launch Minecraft

That's it! The mod is now patched and will work with ForgeCompat.

## Visual Example

**Before:**
```toml
[[dependencies.example_mod]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.19.2,1.20)"
    ordering="NONE"
    side="BOTH"
```

**After:**
```toml
[[dependencies.example_mod]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.19.2,1.21)"
    ordering="NONE"
    side="BOTH"
```

## Common versionRange Patterns

Here are the common patterns you might see and what to change them to:

| Original | Change To |
|----------|-----------|
| `[1.19.2,1.20)` | `[1.19.2,1.21)` |
| `[1.19,1.20)` | `[1.19,1.21)` |
| `1.19.2` | `[1.19.2,1.21)` |
| `[1.19.2]` | `[1.19.2,1.21)` |

**The key change:** Replace the upper bound `1.20` or `1.20)` with `1.21` or `1.21)`

## Troubleshooting

### "I can't find the Minecraft dependency"
Some mods have multiple dependency sections. Look for the one with `modId="minecraft"`. There might be other dependencies for Forge, etc. - only change the Minecraft one.

### "The mod still doesn't load"
Make sure you:
1. Saved the mods.toml file
2. Replaced it back in the JAR (not just copied it)
3. Have ForgeCompat installed
4. Are running Minecraft 1.20.1 with Forge

### "I broke the mod"
If the mod doesn't work after patching, you can:
1. Re-download the original mod JAR
2. Try the patching steps again
3. Make sure you only changed the `versionRange` line

## What This Does

By changing `versionRange="[1.19.2,1.20)"` to `versionRange="[1.19.2,1.21)"`, you're telling Forge:

- **Before:** "This mod only works with Minecraft 1.19.2 up to (but not including) 1.20"
- **After:** "This mod works with Minecraft 1.19.2 up to (but not including) 1.21"

Since 1.20.1 is between 1.19.2 and 1.21, Forge will now accept the mod. ForgeCompat's transformer will then handle the actual code translation at runtime.

## Do I Need to Do This for Every Mod?

**Yes, but only once per mod.** After you patch a mod, it's permanently patched. You won't need to do it again unless you re-download the mod.

## Is This Safe?

**Yes.** You're only changing the version requirement metadata. The actual mod code remains unchanged. ForgeCompat will translate the code at runtime.

## Platform-Specific Instructions

### Windows (using 7-Zip)
1. Right-click the JAR → 7-Zip → Open archive
2. Navigate to META-INF
3. Double-click mods.toml to extract it
4. Edit with Notepad
5. Drag the edited file back into the 7-Zip window
6. Click "Yes" to confirm replacement

### Mac (using Archive Utility)
1. Double-click the JAR to extract it
2. Navigate to the extracted folder → META-INF
3. Right-click mods.toml → Open With → TextEdit
4. Make the change and save
5. Drag the edited file back into the JAR (you may need to use a ZIP editor)

### Linux (using command line)
```bash
# Extract mods.toml
unzip mod.jar META-INF/mods.toml

# Edit it
nano META-INF/mods.toml

# Update the JAR
zip -u mod.jar META-INF/mods.toml

# Clean up
rm -rf META-INF
```

## Need Help?

If you're stuck:
1. Check the [THE_PATCHING_PROBLEM.md](THE_PATCHING_PROBLEM.md) for why this is necessary
2. Make sure you have ForgeCompat installed
3. Check the Minecraft logs for `[ForgeCompat]` messages

That's it! You've successfully patched a 1.19.2 mod to work with ForgeCompat.
