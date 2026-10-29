# Why Patching is Necessary (and Your Options)

## The Problem

When you install a 1.19.2 mod in Minecraft 1.20.1, Forge rejects it during mod discovery because the mod declares it needs Minecraft 1.19.2. This happens **before** ForgeCompat even loads, so we can't intercept it from within the mod.

## Why Can't ForgeCompat Do This Automatically?

Forge's loading order is:
1. **Mod discovery** - finds JARs, reads `mods.toml`
2. **Dependency validation** - checks version requirements ← **1.19.2 mods rejected here**
3. **Mod loading** - loads classes, runs CoreMods
4. **Mod construction** - calls `@Mod` constructors ← **ForgeCompat runs here**

By the time ForgeCompat loads (step 3), Forge has already rejected the 1.19.2 mods (step 2). We can't patch things from within ForgeCompat because we haven't loaded yet.

## Your Options

### Option 1: Python Patcher Script (Recommended)

**Pros:**
- Automatic
- Works with any launcher
- One-time setup per mod

**Cons:**
- Requires downloading and running a script
- Some users may be wary of running scripts

**How to use:**
1. Download `patch_mods.py` from the ForgeCompat release
2. Place it in your mods folder with your 1.19.2 mods
3. Run `python patch_mods.py .`
4. Delete the script

### Option 2: Java Patcher JAR

**Pros:**
- No Python required
- More trusted (it's Java, like Minecraft)
- Automatic

**Cons:**
- Still requires downloading and running something
- Requires Java to be installed (but you already have it for Minecraft)

**How to use:**
1. Download `forgecompat-patcher.jar` from the ForgeCompat release
2. Place it in your mods folder with your 1.19.2 mods
3. Run `java -jar forgecompat-patcher.jar`
4. Delete the patcher JAR

### Option 3: Manual Editing

**Pros:**
- No external tools needed
- Complete control
- No trust issues

**Cons:**
- Tedious for multiple mods
- Requires understanding of mods.toml format

**How to do it:**
1. Open the 1.19.2 mod JAR with a ZIP editor (like 7-Zip)
2. Extract `META-INF/mods.toml`
3. Find the Minecraft dependency:
   ```toml
   [[dependencies.modid]]
       modId="minecraft"
       mandatory=true
       versionRange="[1.19.2,1.20)"
   ```
4. Change `versionRange` to `"[1.19.2,1.21)"`
5. Save and put it back in the JAR

### Option 4: Wait for Mod Authors to Update

**Pros:**
- No patching needed
- Official support

**Cons:**
- Many 1.19.2 mods are abandoned
- You're at the mercy of mod authors
- Could take months or never happen

## Future: Fully Automatic Patching

In the future, ForgeCompat might include:
- **Mixin-based patching** - Patch Forge's dependency validation to be more lenient
- **Custom mod locator** - Intercept mod discovery and patch on-the-fly
- **Forge PR** - Contribute a feature to Forge that allows compatibility layers

These would make patching fully automatic, but they're complex to implement and require deep knowledge of Forge internals.

## Recommendation

For now, **use the Python patcher** (Option 1) or **Java patcher** (Option 2). They're the most reliable and work with all launchers.

If you're uncomfortable running scripts, use **manual editing** (Option 3). It's more work but gives you complete control.

The patching only needs to be done **once per mod**. After that, the mod is permanently patched and will work with ForgeCompat.
