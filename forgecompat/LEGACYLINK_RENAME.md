# LegacyLink - Renamed from ForgeCompat

## What Changed

The mod has been renamed from **ForgeCompat** to **LegacyLink** to better reflect its purpose as a bridge between legacy (1.19.2) and modern (1.20.1) Minecraft Forge mods.

### Package Changes
- **Old:** `net.devmaster.forgecompat`
- **New:** `net.devmaster.legacylink`

### Mod ID Changes
- **Old:** `forgecompat`
- **New:** `legacylink`

### File Changes
All source files have been moved to the new package structure:
- `LegacyLink.java` - Main mod class
- `RuntimeAdapter.java` - Runtime event handlers
- `LegacyLinkTransformer.java` - Bytecode transformer (renamed from ForgeCompatTransformer)
- `TranslationRules.java` - Translation rules
- `MaterialData.java` - Material data holder
- `CompatHelpers.java` - Helper methods
- `CreativeModeTabCompat.java` - CreativeModeTab compatibility shim

## CreativeModeTab Fix

### The Problem
Cait's Curiosities (and other 1.19.2 mods) failed to load with:
```
java.lang.NoSuchMethodError: 'void net.minecraft.world.item.CreativeModeTab.<init>(java.lang.String)'
```

This occurred because the CreativeModeTab API changed significantly between 1.19.2 and 1.20.1.

### What Changed in Minecraft

**1.19.2 API:**
```java
// Old pattern - anonymous subclass with String constructor
CreativeModeTab tab = new CreativeModeTab("my_tab") {
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(MY_ITEM.get());
    }
};
```

**1.20.1 API:**
```java
// New pattern - builder pattern
CreativeModeTab tab = CreativeModeTab.builder()
    .title(Component.translatable("itemGroup.my_tab"))
    .icon(() -> new ItemStack(MY_ITEM.get()))
    .build();
```

The old `CreativeModeTab(String)` constructor was completely removed and replaced with a builder pattern.

### The Solution

Created a compatibility shim (`CreativeModeTabCompat`) that bridges the old and new APIs:

1. **Abstract class** that accepts the old `String` constructor
2. **Internally uses** the new builder pattern to create a real `CreativeModeTab`
3. **Provides** the `makeIcon()` method that old mods override
4. **Wraps** the new API while exposing the old API

### How the Transformer Handles It

When the transformer encounters code like:
```java
new CreativeModeTab("my_tab") {
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(MY_ITEM.get());
    }
}
```

It transforms:
1. **Superclass rename:** The anonymous class now extends `CreativeModeTabCompat` instead of `CreativeModeTab`
2. **Constructor redirect:** The `super("my_tab")` call now calls `CreativeModeTabCompat(String)`
3. **Method compatibility:** The `makeIcon()` override still works because `CreativeModeTabCompat` defines this method

The `CreativeModeTabCompat` constructor then:
1. Takes the tab name
2. Uses the new builder pattern internally
3. Creates a real `CreativeModeTab` instance
4. Returns it via `getTab()`

### Implementation Details

**TranslationRules.java:**
```java
public static final Map<String, String> SUPERCLASS_RENAMES = new HashMap<>();
static {
    // CreativeModeTab - redirect anonymous subclasses to our compat class
    SUPERCLASS_RENAMES.put(
        "net/minecraft/world/item/CreativeModeTab",
        "net/devmaster/legacylink/shim/CreativeModeTabCompat"
    );
}
```

**LegacyLinkTransformer.java:**
```java
// Special handling for CreativeModeTab constructor
if (insn.name.equals("<init>")) {
    if (insn.owner.equals("net/minecraft/world/item/CreativeModeTab")) {
        net.devmaster.legacylink.LegacyLink.LOGGER.info("[LegacyLink] CreativeModeTab constructor detected: {}", insn.desc);
        
        // Redirect all CreativeModeTab constructors to our compat class
        insn.owner = CREATIVE_MODE_TAB_COMPAT;
        return true;
    }
}
```

## Testing

After rebuilding LegacyLink:
1. Place the new JAR in your mods folder
2. Launch Minecraft with Cait's Curiosities
3. Check logs for:
   ```
   [LegacyLink] Found CreativeModeTab subclass: ...
   [LegacyLink] Superclass rename: net/minecraft/world/item/CreativeModeTab → net/devmaster/legacylink/shim/CreativeModeTabCompat
   [LegacyLink] CreativeModeTab constructor detected: (Ljava/lang/String;)V
   ```
4. The creative tab should now appear in-game

## Next Steps

If there are other errors after fixing CreativeModeTab, we can apply similar patterns:
- **Fluid API changes** - Similar shim for fluid registration
- **Recipe system changes** - Shim for old recipe types
- **Network packet changes** - Shim for old packet handling

The key is identifying the old API pattern and creating a compatibility layer that translates it to the new API while maintaining the old interface.
