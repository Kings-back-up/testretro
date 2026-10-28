package net.minecraftforge.compat.shim.material;

import net.minecraft.world.level.material.MapColor;

/**
 * Shim for the 1.19.2 MaterialColor class.
 * 
 * In 1.20.1, MaterialColor was renamed to MapColor.
 * This class provides the old MaterialColor constants that delegate to MapColor.
 * 
 * The transformer rewrites references to MaterialColor → this class,
 * which in turn delegates to MapColor.
 */
public class MaterialColor {
    
    // Direct mappings from 1.19.2 MaterialColor to 1.20.1 MapColor
    // MaterialColor was just renamed to MapColor, values are the same
    
    public static final MapColor NONE = MapColor.NONE;
    public static final MapColor GRASS = MapColor.GRASS;
    public static final MapColor SAND = MapColor.SAND;
    public static final MapColor WOOL = MapColor.WOOL;
    public static final MapColor FIRE = MapColor.FIRE;
    public static final MapColor ICE = MapColor.ICE;
    public static final MapColor METAL = MapColor.METAL;
    public static final MapColor PLANT = MapColor.PLANT;
    public static final MapColor SNOW = MapColor.SNOW;
    public static final MapColor CLAY = MapColor.CLAY;
    public static final MapColor DIRT = MapColor.DIRT;
    public static final MapColor STONE = MapColor.STONE;
    public static final MapColor WATER = MapColor.WATER;
    public static final MapColor WOOD = MapColor.WOOD;
    public static final MapColor QUARTZ = MapColor.QUARTZ;
    public static final MapColor COLOR_ORANGE = MapColor.COLOR_ORANGE;
    public static final MapColor COLOR_MAGENTA = MapColor.COLOR_MAGENTA;
    public static final MapColor COLOR_LIGHT_BLUE = MapColor.COLOR_LIGHT_BLUE;
    public static final MapColor COLOR_YELLOW = MapColor.COLOR_YELLOW;
    public static final MapColor COLOR_LIGHT_GREEN = MapColor.COLOR_LIGHT_GREEN;
    public static final MapColor COLOR_PINK = MapColor.COLOR_PINK;
    public static final MapColor COLOR_GRAY = MapColor.COLOR_GRAY;
    public static final MapColor COLOR_LIGHT_GRAY = MapColor.COLOR_LIGHT_GRAY;
    public static final MapColor COLOR_CYAN = MapColor.COLOR_CYAN;
    public static final MapColor COLOR_PURPLE = MapColor.COLOR_PURPLE;
    public static final MapColor COLOR_BLUE = MapColor.COLOR_BLUE;
    public static final MapColor COLOR_BROWN = MapColor.COLOR_BROWN;
    public static final MapColor COLOR_GREEN = MapColor.COLOR_GREEN;
    public static final MapColor COLOR_RED = MapColor.COLOR_RED;
    public static final MapColor COLOR_BLACK = MapColor.COLOR_BLACK;
    public static final MapColor GOLD = MapColor.GOLD;
    public static final MapColor DIAMOND = MapColor.DIAMOND;
    public static final MapColor LAPIS = MapColor.LAPIS;
    public static final MapColor EMERALD = MapColor.EMERALD;
    public static final MapColor PODZOL = MapColor.PODZOL;
    public static final MapColor NETHER = MapColor.NETHER;
    public static final MapColor TERRACOTTA_WHITE = MapColor.TERRACOTTA_WHITE;
    public static final MapColor TERRACOTTA_ORANGE = MapColor.TERRACOTTA_ORANGE;
    public static final MapColor TERRACOTTA_MAGENTA = MapColor.TERRACOTTA_MAGENTA;
    public static final MapColor TERRACOTTA_LIGHT_BLUE = MapColor.TERRACOTTA_LIGHT_BLUE;
    public static final MapColor TERRACOTTA_YELLOW = MapColor.TERRACOTTA_YELLOW;
    public static final MapColor TERRACOTTA_LIGHT_GREEN = MapColor.TERRACOTTA_LIGHT_GREEN;
    public static final MapColor TERRACOTTA_PINK = MapColor.TERRACOTTA_PINK;
    public static final MapColor TERRACOTTA_GRAY = MapColor.TERRACOTTA_GRAY;
    public static final MapColor TERRACOTTA_LIGHT_GRAY = MapColor.TERRACOTTA_LIGHT_GRAY;
    public static final MapColor TERRACOTTA_CYAN = MapColor.TERRACOTTA_CYAN;
    public static final MapColor TERRACOTTA_PURPLE = MapColor.TERRACOTTA_PURPLE;
    public static final MapColor TERRACOTTA_BLUE = MapColor.TERRACOTTA_BLUE;
    public static final MapColor TERRACOTTA_BROWN = MapColor.TERRACOTTA_BROWN;
    public static final MapColor TERRACOTTA_GREEN = MapColor.TERRACOTTA_GREEN;
    public static final MapColor TERRACOTTA_RED = MapColor.TERRACOTTA_RED;
    public static final MapColor TERRACOTTA_BLACK = MapColor.TERRACOTTA_BLACK;
    public static final MapColor CRIMSON_NYLIUM = MapColor.CRIMSON_NYLIUM;
    public static final MapColor CRIMSON_STEM = MapColor.CRIMSON_STEM;
    public static final MapColor CRIMSON_HYPHAE = MapColor.CRIMSON_HYPHAE;
    public static final MapColor WARPED_NYLIUM = MapColor.WARPED_NYLIUM;
    public static final MapColor WARPED_STEM = MapColor.WARPED_STEM;
    public static final MapColor WARPED_HYPHAE = MapColor.WARPED_HYPHAE;
    public static final MapColor WARPED_WART_BLOCK = MapColor.WARPED_WART_BLOCK;
    public static final MapColor DEEPSLATE = MapColor.DEEPSLATE;
    public static final MapColor RAW_IRON = MapColor.RAW_IRON;
    public static final MapColor GLOW_LICHEN = MapColor.GLOW_LICHEN;
    
    /**
     * In 1.19.2, MaterialColor had a calculateRGB method.
     * In 1.20.1, this is MapColor#col.
     */
    public static int calculateRGB(MapColor color, int brightness) {
        return color.col;
    }
}
