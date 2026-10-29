# The Patching Problem - Honest Explanation

## The Question

> "Can we just do it from the mod itself instead of running separate files?"

## The Honest Answer

**No, not easily.** Here's why:

### Forge's Loading Order

```
1. Mod Discovery     → Forge finds JARs, reads mods.toml
2. Dependency Check  → Forge validates version requirements ← 1.19.2 mods REJECTED HERE
3. Mod Loading       → Forge loads classes, runs CoreMods
4. Mod Construction  → Forge calls @Mod constructors ← ForgeCompat runs HERE
```

By the time ForgeCompat loads (step 4), Forge has already rejected the 1.19.2 mods (step 2). We can't intercept it from within ForgeCompat because we haven't loaded yet.

### Why CoreMods Don't Help

CoreMods run during step 3 (mod loading), but dependency validation happens in step 2. So even CoreMods are too late.

### What Would Work

To make this fully automatic, we'd need to:

1. **Use Mixin** - Patch Forge's dependency validation code
   - Complex to set up
   - Requires deep knowledge of Forge internals
   - May break between Forge versions

2. **Custom Mod Locator** - Intercept mod discovery
   - Even more complex
   - Requires understanding Forge's mod loading system

3. **Forge PR** - Contribute a feature to Forge
   - Would take months to get accepted
   - Not guaranteed to be accepted

4. **Java Agent** - Run code before Forge starts
   - Requires users to add JVM arguments
   - Complex setup
   - Not user-friendly

None of these are simple solutions.

## The Reality

**You have to patch the mods somehow.** The options are:

### Option 1: Python Script
- ✅ Automatic
- ✅ Works with all launchers
- ❌ Requires downloading and running a script
- ❌ Trust issues for some users

### Option 2: Java Patcher JAR
- ✅ Automatic
- ✅ More trusted (it's Java)
- ❌ Still requires downloading and running something
- ❌ Requires Java (but you already have it)

### Option 3: Manual Editing
- ✅ No external tools
- ✅ Complete control
- ❌ Tedious for multiple mods
- ❌ Requires understanding mods.toml

### Option 4: Wait for Mod Updates
- ✅ No patching needed
- ❌ Many 1.19.2 mods are abandoned
- ❌ Could take months or never happen

## Recommendation

**Use the Java patcher JAR** (Option 2). It's:
- More trusted than Python scripts
- Automatic
- Works with all launchers
- Only needs to be run once per mod

The patcher JAR is built into ForgeCompat, so you get both:
- `forgecompat-0.1.0.jar` - The actual compatibility layer
- `forgecompat-patcher-1.0.0.jar` - The standalone patcher

Users download both, run the patcher once, then delete it.

## Future Possibilities

If there's enough demand, we could:
- Investigate Mixin-based patching for fully automatic operation
- Create a GUI tool that makes patching more user-friendly
- Work with Forge to add official support for compatibility layers

But for now, the patcher approach is the most practical solution.

## TL;DR

Forge validates mod versions before ForgeCompat loads, so we can't do it automatically from within the mod. Users need to run a patcher (Python script or Java JAR) once per mod. This is a limitation of how Forge works, not a design choice.

The Java patcher JAR is the recommended solution - it's automatic, trusted, and works with all launchers.
