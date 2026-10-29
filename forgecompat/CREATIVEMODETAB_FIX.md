# CreativeModeTab Compatibility Fix

## Problem

Cait's Curiosities (and likely other 1.19.2 mods) failed to load with:
```
java.lang.NoSuchMethodError: 'void net.minecraft.world.item.CreativeModeTab.<init>(java.lang.String)'
```

This occurred because the CreativeModeTab API changed significantly between 1.19.2 and 1.20.1.

## What Changed

### 1.19.2 API
```java
// Old pattern - anonymous subclass with String constructor
CreativeModeTab tab = new CreativeModeTab("my_tab") {
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(MY_ITEM.get());
    }
};
```

### 1.20.1 API
```java
// New pattern - builder pattern
CreativeModeTab tab = CreativeModeTab.builder()
    .title(Component.translatable("itemGroup.my_tab"))
    .icon(() -> new ItemStack(MY_ITEM.get()))
    .build();
```

The old `CreativeModeTab(String)` constructor was completely removed and replaced with a builder pattern.

## Solution

Created a compatibility shim that bridges the old and new APIs:

### 1. CreativeModeTabCompat Class
**File:** `src/main/java/net/devmaster/forgecompat/shim/CreativeModeTabCompat.java`

This abstract class:
- Accepts the old `String` constructor
- Internally uses the new builder pattern to create a real `CreativeModeTab`
- Provides the `makeIcon()` method that old mods override
- Wraps the new API while exposing the old API

### 2. Transformer Updates

**File:** `src/main/java/net/devmaster/forgecompat/translate/TranslationRules.java`

Added `SUPERCLASS_RENAMES` map:
```java
SUPERCLASS_RENAMES.put(
    "net/minecraft/world/item/CreativeModeTab",
    "net/devmaster/forgecompat/shim/CreativeModeTabCompat"
);
```

This is separate from `CLASS_RENAMES` because we only want to rename CreativeModeTab when it's used as a superclass, not in all contexts.

**File:** `src/main/java/net/devmaster/forgecompat/transformer/ForgeCompatTransformer.java`

Updated `remapClassHierarchy()` to check both `CLASS_RENAMES` and `SUPERCLASS_RENAMES`.

Added special handling in `transformMethodCall()` for the constructor:
```java
if (insn.name.equals("<init>") && 
    insn.owner.equals("net/minecraft/world/item/CreativeModeTab") &&
    insn.desc.equals("(Ljava/lang/String;)V")) {
    
    insn.owner = "net/devmaster/forgecompat/shim/CreativeModeTabCompat";
    return true;
}
```

## How It Works

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

## Testing

After rebuilding ForgeCompat:
1. Place the new JAR in your mods folder
2. Launch Minecraft with Cait's Curiosities
3. Check logs for: `[ForgeCompat] Superclass rename: net/minecraft/world/item/CreativeModeTab → net/devmaster/forgecompat/shim/CreativeModeTabCompat`
4. The creative tab should now appear in-game

## Future Improvements

This pattern can be extended for other API changes:
- **Fluid API changes** - Similar shim for fluid registration
- **Recipe system changes** - Shim for old recipe types
- **Network packet changes** - Shim for old packet handling

The key is identifying the old API pattern and creating a compatibility layer that translates it to the new API while maintaining the old interface.
